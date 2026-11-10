# Project 2: JavaScript Injection, eval() Injection, and Their Mitigation

MSCS-535 Secure Software Development, Fall 2026. Ananya Solanki.

Chapters 5-8 of Richardson and Thies, Secure Software Design. Node.js 22 and Express 5.

The two vulnerable apps are intentionally insecure. Run them only on 127.0.0.1.

## Files

| File | Rubric part | What it does |
| --- | --- | --- |
| `xss-vulnerable.js` | 1. Code injection via web applications | Express app on port 3001 with reflected XSS (`/search`), stored XSS (`/guestbook`), and DOM-based XSS (`/welcome`) |
| `eval-vulnerable.js` | 2. Dynamic evaluation at runtime | Express app on port 3002: `eval()` calculator (`/calc`), a blocklist that can be bypassed (`/calc-filtered`), and `new Function()` settings parser (`/settings`) |
| `secure-app.js` | 3. Mitigation and Content Security Policy | Port 3003. Output encoding, server-side validation, `textContent`, HttpOnly cookie, nonce-based CSP, an arithmetic parser in place of `eval()`, `JSON.parse` with an allowlist in place of `new Function()` |
| `attack-demo.js` | All parts | Sends every attack to all three apps and prints the result |
| `browser-check.py` | Parts 1 and 3 | Opens each attack in headless Chromium to show whether injected JavaScript really runs, and tests the CSP on its own |
| `demo_output.txt`, `browser_output.txt` | Evidence | Real output of the two checks |

## Run

```
npm install
npm run demo                 # node attack-demo.js
pip install playwright && python -m playwright install chromium
python browser-check.py      # real-browser check
```

To click through the apps by hand, start one at a time and open it in a browser:

```
npm run vulnerable-xss       # http://127.0.0.1:3001/search?q=<script>alert(1)</script>
npm run vulnerable-eval      # http://127.0.0.1:3002/calc?expr=6*7
npm run secure               # http://127.0.0.1:3003 (started with --disallow-code-generation-from-strings)
```

## Note on Express 4

With Express 4, `node --disallow-code-generation-from-strings` stops the server at startup, because the `depd` package that Express 4 loads calls `new Function()`. Express 5 does not, so this project uses Express 5.

GitHub Actions runs both checks on every push (`.github/workflows/build.yml`).
