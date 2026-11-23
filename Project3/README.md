# Project 3: Securing an Online Payment System (Chapters 9-12)

Pseudocode for the checkout of an online store, protected against SQL injection and cross-site scripting (XSS). Course: MSCS-535-B01 Secure Software Development.

The pseudocode does not depend on any one language. Read `<-` as assignment, `?` as a query placeholder, and `//` as a comment. Each file is one figure in the Word report, and the line numbers in the report match the line numbers in these files.

| File | Figure | Purpose |
|---|---|---|
| `01_secure_checkout.txt` | Figure 1 | Checkout request handler: HTTPS, session, CSRF token, rate limit, dirty-to-clean input, server-side price, tokenized card, transaction, safe errors |
| `02_vulnerable_vs_secure.txt` | Figure 2 | The same order lookup written unsafely (string concatenation) and safely (parameterized) |
| `03_input_validation.txt` | Figure 3 | Allow-list validation, with length checked first |
| `04_data_access.txt` | Figure 4 | Parameterized INSERT, UPDATE, and SELECT; allow-listed ORDER BY; LIKE wildcard escaping; least-privilege account |
| `05_output_encoding.txt` | Figure 5 | Context-aware output encoding (HTML, attribute, URL); receipt and search pages with no raw-HTML path |
| `06_headers_and_browser.txt` | Figure 6 | CSP with nonce, strict-dynamic, and Trusted Types; HSTS and other headers; cookie flags; SRI; textContent; payment-page integrity monitor (PCI DSS 4.0.1 Req. 6.4.3 and 11.6.1) |

## Defense summary

**SQL injection:** allow-list validation, then parameterized queries for every value, then allow-list maps for identifiers that cannot be parameterized, then a least-privilege database account with generic error messages.

**XSS:** context-aware output encoding through `SetText()` and `SetAttr()` only, then `textContent` in browser code, then a nonce-based Content Security Policy with Trusted Types, then HttpOnly cookies, then Subresource Integrity and tamper monitoring on the payment page.

## Main references

- Richardson, T., & Thies, C. N. (2012). *Secure software design*. Jones & Bartlett Learning.
- OWASP Foundation. *SQL Injection Prevention Cheat Sheet* and *Cross Site Scripting Prevention Cheat Sheet*.
- PCI Security Standards Council. (2024). *PCI DSS v4.0.1*.
