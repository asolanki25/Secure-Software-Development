// Project 2, Part 3: Mitigation of XSS and eval injection, with a Content Security Policy
// Start with: node --disallow-code-generation-from-strings secure-app.js
// MSCS-535 Secure Software Development, Fall 2026 - Ananya Solanki
const express = require('express');
const crypto = require('crypto');

// --- XSS defense 1: output encoding (HTML context) ---------------------------
function escapeHtml(value) {
  const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
  return String(value).replace(/[&<>"']/g, (ch) => map[ch]);
}

// --- XSS defense 2: server-side input validation (length first, then format) --
const SEARCH_FORMAT = /^[A-Za-z0-9 .,'-]{1,50}$/;      // allowlist for search terms
const MAX_COMMENT = 500;

// --- eval defense: a small arithmetic parser instead of eval() ---------------
function evaluateArithmetic(input) {
  if (typeof input !== 'string' || input.length === 0 || input.length > 100) throw new Error('length');
  if (!/^[0-9+\-*/(). ]+$/.test(input)) throw new Error('characters');
  const tokens = input.match(/\d+(\.\d+)?|[+\-*/()]/g) || [];
  if (tokens.join('') !== input.replace(/ /g, '')) throw new Error('syntax');
  let pos = 0;
  const peek = () => tokens[pos];
  const take = () => tokens[pos++];
  function expression() {                              // expression := term (('+'|'-') term)*
    let v = term();
    while (peek() === '+' || peek() === '-') v = take() === '+' ? v + term() : v - term();
    return v;
  }
  function term() {                                    // term := factor (('*'|'/') factor)*
    let v = factor();
    while (peek() === '*' || peek() === '/') {
      const op = take();
      const r = factor();
      if (op === '/' && r === 0) throw new Error('divide by zero');
      v = op === '*' ? v * r : v / r;
    }
    return v;
  }
  function factor() {                                  // factor := number | '-' factor | '(' expression ')'
    const t = take();
    if (t === '-') return -factor();
    if (t === '(') {
      const v = expression();
      if (take() !== ')') throw new Error('parenthesis');
      return v;
    }
    if (t !== undefined && /^\d/.test(t)) return Number(t);
    throw new Error('syntax');
  }
  const value = expression();
  if (pos !== tokens.length || !Number.isFinite(value)) throw new Error('syntax');
  return value;
}

// --- eval defense: JSON.parse plus an allowlist schema instead of new Function() ---
function parseSettings(text) {
  const data = JSON.parse(text);                       // data only, never code
  const themes = ['light', 'dark'];
  if (typeof data !== 'object' || data === null || Array.isArray(data)) throw new Error('shape');
  for (const key of Object.keys(data)) {
    if (!['theme', 'fontSize'].includes(key)) throw new Error('unknown key');
  }
  if (!themes.includes(data.theme)) throw new Error('theme');
  if (!Number.isInteger(data.fontSize) || data.fontSize < 10 || data.fontSize > 24) throw new Error('fontSize');
  return { theme: data.theme, fontSize: data.fontSize };
}

function createApp() {
  const app = express();
  app.disable('x-powered-by');
  app.use(express.urlencoded({ extended: false, limit: '2kb' }));
  app.use(express.json({ limit: '2kb' }));
  const comments = [];

  // Content Security Policy with a fresh nonce on every response.
  app.use((req, res, next) => {
    res.locals.nonce = crypto.randomBytes(16).toString('base64');
    res.set('Content-Security-Policy', [
      "default-src 'self'",
      `script-src 'nonce-${res.locals.nonce}'`,        // no 'unsafe-inline', no 'unsafe-eval'
      "object-src 'none'",
      "base-uri 'none'",
      "frame-ancestors 'none'",
      "form-action 'self'",
    ].join('; '));
    res.set('X-Content-Type-Options', 'nosniff');
    next();
  });

  // Fix for reflected XSS: validate on the server, then encode on output.
  app.get('/search', (req, res) => {
    const q = req.query.q;
    if (typeof q !== 'string' || !SEARCH_FORMAT.test(q)) {
      return res.status(400).send('<h1>Invalid search term</h1>');
    }
    res.send(`<h1>Results for: ${escapeHtml(q)}</h1><p>No products matched.</p>`);
  });

  // Fix for stored XSS: limit length on input, encode every comment on output.
  app.post('/comment', (req, res) => {
    const text = req.body.text;
    if (typeof text !== 'string' || text.length === 0 || text.length > MAX_COMMENT) {
      return res.status(400).send('<h1>Comment rejected</h1>');
    }
    comments.push(text);                               // stored as data, encoded when shown
    res.redirect('/guestbook');
  });
  app.get('/guestbook', (req, res) => {
    res.cookie('session', 'demo-session-123', { httpOnly: true, sameSite: 'strict' });
    const list = comments.map((c) => `<li>${escapeHtml(c)}</li>`).join('');
    res.send(`<h1>Guestbook</h1><ul>${list}</ul>
      <form method="post" action="/comment">
        <input name="text" maxlength="${MAX_COMMENT}"><button>Post</button>
      </form>`);
  });

  // Fix for DOM-based XSS: textContent instead of innerHTML; script carries the nonce.
  app.get('/welcome', (req, res) => {
    res.send(`<h1 id="greet"></h1>
      <script nonce="${res.locals.nonce}">
        const name = decodeURIComponent(location.hash.slice(1)).slice(0, 40);
        document.getElementById('greet').textContent = 'Welcome, ' + name;
      </script>`);
  });

  // Fix for eval(): parse arithmetic, never execute it.
  app.get('/calc', (req, res) => {
    try {
      res.send(`Result: ${evaluateArithmetic(req.query.expr)}`);
    } catch (e) {
      res.status(400).send('Bad expression');          // one generic message
    }
  });

  // Fix for new Function(): JSON.parse plus an allowlist schema.
  app.post('/settings', (req, res) => {
    try {
      res.json({ saved: parseSettings(String(req.body.data)) });
    } catch (e) {
      res.status(400).json({ error: 'Bad settings' });
    }
  });

  return app;
}

if (require.main === module) {
  createApp().listen(3003, '127.0.0.1',
    () => console.log('Secure app: http://127.0.0.1:3003'));
}
module.exports = { createApp, escapeHtml, evaluateArithmetic, parseSettings };
