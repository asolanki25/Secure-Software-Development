// Project 2, Part 2: Dynamic evaluation of code at runtime (eval injection)
// INTENTIONALLY VULNERABLE - run only on 127.0.0.1 for the demonstration.
// MSCS-535 Secure Software Development, Fall 2026 - Ananya Solanki
const express = require('express');

function createApp() {
  const app = express();
  app.use(express.json());

  // Flaw 1 - eval(): a "calculator" runs the expression string as JavaScript.
  // Direct eval also sees this module's local scope, including require().
  app.get('/calc', (req, res) => {
    const expr = req.query.expr || '';
    try {
      const result = eval(expr);                       // user input becomes code
      res.send(`Result: ${result}`);
    } catch (e) {
      res.status(400).send('Bad expression');
    }
  });

  // Flaw 2 - a blocklist "fix": it rejects dangerous words but still calls eval().
  app.get('/calc-filtered', (req, res) => {
    const expr = req.query.expr || '';
    if (/require|process|child_process|import/i.test(expr)) {
      return res.status(400).send('Blocked');
    }
    try {
      res.send(`Result: ${eval(expr)}`);
    } catch (e) {
      res.status(400).send('Bad expression');
    }
  });

  // Flaw 3 - the Function constructor used to "parse" settings from the client.
  // new Function(text) compiles text into code, exactly like eval().
  app.post('/settings', (req, res) => {
    try {
      const settings = new Function('return ' + req.body.data)();
      res.json({ saved: settings });
    } catch (e) {
      res.status(400).json({ error: 'Bad settings' });
    }
  });

  return app;
}

if (require.main === module) {
  createApp().listen(3002, '127.0.0.1',
    () => console.log('Vulnerable eval app: http://127.0.0.1:3002'));
}
module.exports = { createApp };
