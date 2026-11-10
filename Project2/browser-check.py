"""Project 2: real-browser check of the XSS attacks and the Content Security Policy.

Starts xss-vulnerable.js (port 3001) and secure-app.js (port 3003), opens each attack
URL in headless Chromium, and reports whether injected JavaScript actually ran
(a JavaScript alert box appeared) or was blocked.
MSCS-535 Secure Software Development, Fall 2026 - Ananya Solanki
"""
import os
import re
import subprocess
import sys
import time
import urllib.request
from urllib.parse import quote, urlencode

from playwright.sync_api import sync_playwright

HERE = os.path.dirname(os.path.abspath(__file__))
VULN, SAFE = "http://127.0.0.1:3001", "http://127.0.0.1:3003"


def start(script, extra=()):
    proc = subprocess.Popen(["node", *extra, script], cwd=HERE, stdout=subprocess.DEVNULL)
    time.sleep(1.0)
    return proc


def visit(browser, url, post_comment=None, tamper=False):
    """Open a page; return (alert messages, CSP console errors, visible heading text)."""
    page = browser.new_page()
    alerts, csp = [], []
    page.on("dialog", lambda d: (alerts.append(d.message), d.dismiss()))
    page.on("console", lambda m: csp.append(m.text) if "Content Security Policy" in m.text else None)
    if tamper:
        # Simulates a developer forgetting to encode: attack code is inserted into the
        # secure page after it leaves the server, but the page keeps its real CSP header.
        def inject(route):
            resp = route.fetch()
            nonce = re.search(r"nonce-([^']+)'", resp.headers["content-security-policy"]).group(1)
            body = resp.text() + (
                "<script>alert('injected inline script')</script>"
                "<img src=x onerror=\"alert('injected onerror handler')\">"
                f"<script nonce=\"{nonce}\">try {{ eval(\"alert('eval ran')\") }}"
                " catch (e) { console.log('Content Security Policy blocked eval: ' + e.name) }</script>")
            route.fulfill(response=resp, body=body)
        page.route("**/search*", inject)
    if post_comment:   # the attacker posts the comment once; the victim then views the page
        data = urlencode({"text": post_comment}).encode()
        urllib.request.urlopen(url.rsplit("/", 1)[0] + "/comment", data=data)
    page.goto(url)
    page.wait_for_timeout(500)
    heading = page.inner_text("h1") if page.query_selector("h1") else ""
    page.close()
    return alerts, csp, heading


def report(label, result):
    alerts, csp, heading = result
    status = f"RAN - alert: {alerts}" if alerts else "blocked - no alert"
    print(f"  {label:<38} {status}")
    if heading:
        print(f"  {'':<38} page heading: {heading!r}")
    for msg in csp:
        print(f"  {'':<38} console: {msg[:110]}")


def main():
    servers = [start("xss-vulnerable.js"),
               start("secure-app.js", ["--disallow-code-generation-from-strings"])]
    try:
        with sync_playwright() as p:
            chrome = os.environ.get("CHROME_PATH")
            browser = p.chromium.launch(executable_path=chrome) if chrome else p.chromium.launch()
            reflected = "/search?q=" + quote("<script>alert('reflected XSS')</script>")
            dom = "/welcome#" + quote("<img src=x onerror=alert('DOM-XSS')>")
            stored = "<img src=x onerror=alert(document.cookie)>"

            print("Vulnerable app (xss-vulnerable.js) in Chromium")
            report("Reflected XSS", visit(browser, VULN + reflected))
            report("Stored XSS (alert shows the cookie)", visit(browser, VULN + "/guestbook", post_comment=stored))
            report("DOM-based XSS", visit(browser, VULN + dom))

            print("\nSecure app (secure-app.js) in Chromium")
            report("Reflected XSS", visit(browser, SAFE + reflected))
            report("Stored XSS", visit(browser, SAFE + "/guestbook", post_comment=stored))
            report("DOM-based XSS", visit(browser, SAFE + dom))
            report("CSP alone (encoding bypassed)", visit(browser, SAFE + "/search?q=shoes", tamper=True))
            browser.close()
    finally:
        for s in servers:
            s.terminate()


if __name__ == "__main__":
    sys.exit(main())
