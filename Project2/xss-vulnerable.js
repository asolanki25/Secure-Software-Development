// Project 2, Part 1: JavaScript code injection (cross-site scripting)
// INTENTIONALLY VULNERABLE - run only on 127.0.0.1 for the demonstration.
// MSCS-535 Secure Software Development, Fall 2026 - Ananya Solanki
const express = require('express');

function createApp() {
  const app = express();
  app.use(express.urlencoded({ extended: false }));
  const comments = [];                                 // in-memory guestbook

  // Flaw 1 - Reflected XSS: the search term is pasted into the page as-is.
  app.get('/search', (req, res) => {
    const q = req.query.q || '';
    res.send(`<h1>Results for: ${q}</h1><p>No products matched.</p>`);
  });

  // Flaw 2 - Stored XSS: comments are saved raw and shown to every visitor.
  app.post('/comment', (req, res) => {
    comments.push(req.body.text || '');
    res.redirect('/guestbook');
  });
  app.get('/guestbook', (req, res) => {
    res.cookie('session', 'demo-session-123');         // readable by JavaScript
    const list = comments.map((c) => `<li>${c}</li>`).join('');
    res.send(`<h1>Guestbook</h1><ul>${list}</ul>
      <form method="post" action="/comment">
        <input name="text"><button>Post</button>
      </form>`);
  });

  // Flaw 3 - DOM-based XSS: browser code writes the URL fragment into innerHTML.
  app.get('/welcome', (req, res) => {
    res.send(`<h1 id="greet"></h1>
      <script>
        const name = decodeURIComponent(location.hash.slice(1));
        document.getElementById('greet').innerHTML = 'Welcome, ' + name;
      </script>`);
  });

  return app;
}

if (require.main === module) {
  createApp().listen(3001, '127.0.0.1',
    () => console.log('Vulnerable XSS app: http://127.0.0.1:3001'));
}
module.exports = { createApp };
