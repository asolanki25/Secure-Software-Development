# Assignment 8: Java File Reading That Fails Safely

MSCS-535 Secure Software Development, Fall 2026. Ananya Solanki.

## Files

- `SafeFileReader.java`: Question 2. Reads a text file so that any problem ends in a safe state. The method returns the file text only after a complete, checked read. In every other case it returns `Optional.empty()`, the caller sees one generic message, and the detail goes to the internal log.
- `output.txt`: the real program output (Java 17).

## How the code fails safely

| Rule | Where in the code |
| --- | --- |
| Default deny | The method ends with `return Optional.empty();`. Only a fully successful read reaches `Optional.of(...)`. |
| No path traversal (CWE-22) | The requested name is resolved, normalized, and checked with `startsWith(baseDir)`, before and after `toRealPath()` (symlinks resolved). |
| Size limit | Files over 64 KB are refused before reading. |
| Strict decoding | A UTF-8 decoder set to `REPORT` rejects malformed bytes instead of silently replacing them. |
| Resources always closed | `try-with-resources` closes the reader even when an exception is thrown. |
| Specific catches first | `NoSuchFileException`, `AccessDeniedException`, `MalformedInputException`, `SecurityException`, then `IOException`. |
| No information leakage (CWE-209) | The caller gets "The file could not be read." The path and cause go to the log only. |
| Audit trail | The `finally` block logs every request and its outcome. |

## Demonstration cases

| Request | Result | Reason |
| --- | --- | --- |
| `config.txt` | SUCCESS | Valid file inside the base directory |
| `missing.txt` | FAILED SAFELY | File not found |
| `../../etc/passwd` | FAILED SAFELY | Path outside base directory |
| `big.log` | FAILED SAFELY | Over the 64 KB limit |
| `bad.txt` | FAILED SAFELY | Invalid UTF-8 bytes |
| `folder` | FAILED SAFELY | A directory, not a regular file |

## Run

```
javac SafeFileReader.java
java SafeFileReader
```

Requires Java 11 or later. GitHub Actions compiles and runs it on every push (`.github/workflows/build.yml`).
