'use strict';
// Client-side code: helps the user, but the server never trusts it.

const NAME_RE = /^[\p{L}][\p{L} .'-]{0,99}$/u;
const CITY_RE = /^[A-Za-z][A-Za-z .'-]{0,59}$/;
const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[A-Za-z]{2,}$/;
let csrfToken = '';

async function getCsrfToken() {
  const res = await fetch('api.php?action=csrf', { credentials: 'same-origin' });
  csrfToken = (await res.json()).token;
}

function showMessage(el, text, isError) {
  el.textContent = text;                 // text only, never parsed as HTML
  el.className = isError ? 'error' : '';
}

function validate(data) {               // same rules as the server, for UX
  if (!NAME_RE.test(data.full_name)) return 'Please enter a valid name.';
  if (data.email.length > 254 || !EMAIL_RE.test(data.email))
    return 'Please enter a valid email address.';
  if (!CITY_RE.test(data.city)) return 'Please enter a valid city.';
  return '';
}

async function addCustomer(event) {
  event.preventDefault();
  const status = document.getElementById('add-status');
  const form = event.currentTarget;
  const data = {
    full_name: form.full_name.value.trim(),
    email: form.email.value.trim(),
    city: form.city.value.trim(),
  };
  const problem = validate(data);
  if (problem) return showMessage(status, problem, true);

  try {
    const res = await fetch('api.php?action=add', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json',
                 'X-CSRF-Token': csrfToken },
      body: JSON.stringify(data),
    });
    const body = await res.json();
    showMessage(status, body.message, !res.ok);
    if (res.ok) form.reset();
  } catch {
    showMessage(status, 'The request could not be completed.', true);
  }
}

function renderRows(rows) {
  const table = document.getElementById('results');
  const tbody = table.querySelector('tbody');
  tbody.replaceChildren();               // clear old rows safely
  for (const row of rows) {
    const tr = document.createElement('tr');
    for (const value of [row.full_name, row.city, row.created_at]) {
      const td = document.createElement('td');
      td.textContent = value;            // stored input shown as plain text
      tr.appendChild(td);
    }
    tbody.appendChild(tr);
  }
  table.hidden = rows.length === 0;
}

async function searchCustomers(event) {
  event.preventDefault();
  const q = document.getElementById('q').value.trim().slice(0, 50);
  const msg = document.getElementById('search-message');
  const res = await fetch('api.php?action=search&q=' + encodeURIComponent(q),
                          { credentials: 'same-origin' });
  const body = await res.json();
  if (!res.ok) return showMessage(msg, body.message, true);
  renderRows(body.results);
  showMessage(msg, body.results.length === 0
    ? 'No customers match: ' + q              // reflected input, as text
    : body.results.length + ' customer(s) found.', false);
}

document.addEventListener('DOMContentLoaded', () => {
  getCsrfToken();
  document.getElementById('add-form').addEventListener('submit', addCustomer);
  document.getElementById('search-form')
          .addEventListener('submit', searchCustomers);
});
