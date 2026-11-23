"""Loads the site in headless Chrome, uses it like a person would, and checks
that hostile text is displayed as text and never runs as script.
Usage: python3 tests/browser_check.py http://127.0.0.1:8080
"""
import sys
from playwright.sync_api import sync_playwright

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080"
fired = []
csp_blocks = []

with sync_playwright() as p:
    browser = p.chromium.launch(channel="chrome")
    page = browser.new_page(viewport={"width": 900, "height": 900})
    page.on("dialog", lambda d: (fired.append(d.message), d.dismiss()))
    page.on("console", lambda m: csp_blocks.append(m.text)
            if "Content Security Policy" in m.text else None)
    page.goto(BASE + "/")
    page.wait_for_timeout(500)                     # let app.js fetch the token

    # 1. Normal use: add a customer through the form
    page.fill("#full_name", "Priya Nair")
    page.fill("#email", "priya@example.com")
    page.fill("#city", "Hoboken")
    page.click("#add-form button")
    page.wait_for_timeout(500)
    print("Add customer message:", page.text_content("#add-status"))

    # 2. Stored XSS: a hostile name already in the database is searched for
    page.fill("#q", "Legacy")
    page.click("#search-form button")
    page.wait_for_timeout(500)
    cell = page.text_content("#results tbody tr td")
    print("Stored name shown as text:", cell)
    print("img elements inside results:", page.locator("#results img").count())
    page.screenshot(path="tests/screenshot_stored_xss.png", full_page=True)

    # 3. Reflected XSS: hostile search text is echoed back in the message
    page.fill("#q", "<img src=x onerror=alert(2)>")
    page.click("#search-form button")
    page.wait_for_timeout(500)
    print("Reflected message:", page.text_content("#search-message"))
    page.screenshot(path="tests/screenshot_reflected_xss.png", full_page=True)

    # 4. Inline script injected into the page is blocked by the CSP
    page.evaluate("""() => { const s = document.createElement('script');
        s.textContent = "alert('inline')"; document.body.appendChild(s); }""")
    page.wait_for_timeout(300)

    print("CSP violations reported by Chrome:", len(csp_blocks))
    for msg in csp_blocks:
        print("  ", msg[:150])
    print("Alert dialogs fired:", len(fired), fired)
    browser.close()
