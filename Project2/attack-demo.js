// Project 2: runs every attack against the vulnerable apps and the secure app,
// and prints what happened. Everything stays on 127.0.0.1.
// MSCS-535 Secure Software Development, Fall 2026 - Ananya Solanki
const { execFileSync } = require('child_process');
process.env.DB_PASSWORD = 'S3cr3t-demo-password';     // a fake secret for the leak test

const start = (factory) => new Promise((resolve) => {
  const server = factory().listen(0, '127.0.0.1', () => resolve(server));
});
const url = (server, path) => `http://127.0.0.1:${server.address().port}${path}`;
const get = async (s, path) => { const r = await fetch(url(s, path)); return { status: r.status, headers: r.headers, body: await r.text() }; };
const post = async (s, path, body, type) => {
  const r = await fetch(url(s, path), { method: 'POST', redirect: 'manual', headers: { 'Content-Type': type }, body });
  return { status: r.status, body: await r.text() };
};
const show = (label, value) => console.log(`  ${label.padEnd(44)} ${value}`);
const q = encodeURIComponent;

const XSS = '<script>alert(document.cookie)</script>';
const IMG = '<img src=x onerror=alert(document.cookie)>';
const RCE = "require('child_process').execSync('whoami').toString().trim()";
const BYPASS = "({}).constructor.constructor(\"return this['pro'+'cess'].getBuiltinModule('child_pro'+'cess').execSync('whoami').toString().trim()\")()";
const LEAK = '{ theme: "dark", leaked: process.env.DB_PASSWORD }';

(async () => {
  const xss = await start(require('./xss-vulnerable').createApp);
  const ev = await start(require('./eval-vulnerable').createApp);
  const safe = await start(require('./secure-app').createApp);

  console.log('PART 1 - JavaScript injection (XSS) against xss-vulnerable.js');
  let r = await get(xss, `/search?q=${q(XSS)}`);
  show('Reflected: script tag returned unencoded?', r.body.includes(XSS) ? 'YES - script runs in the browser' : 'no');
  await post(xss, '/comment', `text=${q(IMG)}`, 'application/x-www-form-urlencoded');
  r = await get(xss, '/guestbook');
  show('Stored: onerror tag shown to every visitor?', r.body.includes(IMG) ? 'YES - runs for each visitor' : 'no');
  show('Session cookie readable by script?', /HttpOnly/i.test(r.headers.get('set-cookie')) ? 'no' : 'YES - no HttpOnly flag');
  r = await get(xss, '/welcome');
  show('DOM-based: page writes URL into innerHTML?', r.body.includes('.innerHTML =') ? 'YES - /welcome#<img src=x onerror=...>' : 'no');
  show('Content-Security-Policy header', r.headers.get('content-security-policy') || 'NONE');

  console.log('\nPART 2 - Dynamic evaluation (eval) against eval-vulnerable.js');
  r = await get(ev, `/calc?expr=${q('6*7')}`);
  show('Normal use: 6*7', r.body);
  r = await get(ev, `/calc?expr=${q(RCE)}`);
  show('eval(): run an OS command (whoami)', `${r.body}  <- command ran on the server`);
  r = await get(ev, `/calc?expr=${q('process.env.DB_PASSWORD')}`);
  show('eval(): read a server secret', `${r.body}  <- secret leaked`);
  r = await get(ev, `/calc-filtered?expr=${q(RCE)}`);
  show('Blocklist: same payload', r.body);
  r = await get(ev, `/calc-filtered?expr=${q(BYPASS)}`);
  show('Blocklist: payload without blocked words', `${r.body}  <- blocklist bypassed`);
  r = await post(ev, '/settings', JSON.stringify({ data: LEAK }), 'application/json');
  show('new Function(): settings with code inside', `${r.body}  <- secret leaked`);

  console.log('\nPART 3 - The same attacks against secure-app.js');
  r = await get(safe, `/search?q=${q(XSS)}`);
  show('Reflected payload', `HTTP ${r.status} ${r.body}`);
  r = await get(safe, `/search?q=${q('blue shoes')}`);
  show('Normal search', `HTTP ${r.status} ${r.body.replace(/<[^>]+>/g, ' ').replace(/ +/g, ' ').trim()}`);
  await post(safe, '/comment', `text=${q(IMG)}`, 'application/x-www-form-urlencoded');
  r = await get(safe, '/guestbook');
  show('Stored payload is shown as', r.body.match(/<li>(.*?)<\/li>/)[1]);
  show('Raw onerror tag present in page?', r.body.includes(IMG) ? 'YES' : 'no - encoded as text');
  show('Session cookie flags', r.headers.get('set-cookie').split('; ').slice(2).join('; '));
  r = await get(safe, '/welcome');
  show('DOM-based fix uses', r.body.includes('.textContent =') ? 'textContent (no HTML parsing)' : 'innerHTML');
  console.log('  Content-Security-Policy header:');
  r.headers.get('content-security-policy').split('; ').forEach((d) => console.log(`      ${d}`));
  r = await get(safe, `/calc?expr=${q('(2+3)*4-10/5')}`);
  show('Safe parser: (2+3)*4-10/5', r.body);
  for (const [label, p] of [['eval payload: OS command', RCE], ['eval payload: secret', 'process.env.DB_PASSWORD'], ['eval payload: blocklist bypass', BYPASS]]) {
    r = await get(safe, `/calc?expr=${q(p)}`);
    show(label, `HTTP ${r.status} ${r.body}`);
  }
  r = await post(safe, '/settings', JSON.stringify({ data: LEAK }), 'application/json');
  show('Settings with code inside', `HTTP ${r.status} ${r.body}`);
  r = await post(safe, '/settings', JSON.stringify({ data: '{"theme":"dark","fontSize":14}' }), 'application/json');
  show('Valid JSON settings', `HTTP ${r.status} ${r.body}`);

  console.log('\nDEFENSE IN DEPTH - node --disallow-code-generation-from-strings');
  const out = execFileSync(process.execPath, ['--disallow-code-generation-from-strings', '-e',
    "for (const f of [() => eval('1+1'), () => new Function('return 1')()]) { try { f(); console.log('ran'); } catch (e) { console.log(e.name + ': ' + e.message); } }"]).toString().trim().split('\n');
  show('eval(\'1+1\')', out[0]);
  show('new Function(\'return 1\')', out[1]);

  [xss, ev, safe].forEach((s) => s.close());
})();
