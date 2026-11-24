# Assignment 16: Proactive Security Tools (demonstration files)

Two of the five tools described in the paper were run here. The samples are deliberately vulnerable and exist only to show the tools working. Do not reuse them.

| File | Purpose |
|---|---|
| `rules/course-rules.yml` | Local Semgrep rules for the flaws studied in Projects 2 and 3: SQL built by joining strings, `innerHTML`, `eval`, unescaped `echo` |
| `samples/vulnerable_search.php`, `samples/vulnerable_ui.js` | Deliberately vulnerable code for the scan (the AWS key is the public example key from AWS documentation) |
| `samples/requirements-old.txt` | An outdated dependency pin for pip-audit |
| `out_semgrep.txt` | Semgrep results: 4 findings in the samples, 0 in Assignment14 |
| `out_pip_audit.txt` | pip-audit results for `requests==2.19.1` |

## Run it on Linux

```bash
pip install semgrep pip-audit
semgrep scan --metrics=off --config rules/course-rules.yml ../Assignment14 samples
pip-audit -r samples/requirements-old.txt --desc off
# Secret scanning (not run in the paper): https://github.com/gitleaks/gitleaks
gitleaks dir samples
```
