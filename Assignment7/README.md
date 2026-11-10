# Assignment 7: Security UML Diagram for a Software System

MSCS-535 Secure Software Development, Fall 2026. Ananya Solanki.

## Files

- `secure_account_portal_usecase.puml`: Question 2. PlantUML source for a UML use case overview diagram of the Secure Account Portal (the Spring Boot account application from Project 1). It adds security use cases, misuse cases (Sindre and Opdahl, 2005), and the boundary notation from Richardson and Thies (2012), Chapter 8.
- `SecureAccountPortal.png` and `SecureAccountPortal.svg`: the rendered diagram (Figure 1 in the Blackboard post).

## Reading the diagram

| Element | Meaning |
| --- | --- |
| White oval (UC1 to UC5) | Business use case, normal operation |
| Blue oval `<<security>>` (S1 to S6) | Security use case, included in or extending a business use case |
| Black oval `<<misuse>>` (M1 to M4) | Misuse case, a goal of the attacker |
| `THREATENS` | Misuse case aimed at a business use case |
| `MITIGATES` / `DETECTS` | Security use case that blocks or reveals a misuse case |
| `[E]` | Link crosses the external system boundary |
| `[I]` | Internal link between hosts inside the system |
| `[C]` | Link carries critical or sensitive data |
| `[A]` | Potential attack point |

A link marked `[E][C]` is the most likely target for attack (Richardson and Thies, 2012, Chapter 8). In this diagram those are the Customer and Bank Administrator links to Log In, Transfer Funds, and Manage User Roles.

| Misuse case | Threatens | Mitigated by | Fail case exit state |
| --- | --- | --- | --- |
| M1 Stuff or Guess Passwords | UC1 Log In | S1 TOTP, S2 lockout | Generic "invalid credentials" message, no remaining-attempt count |
| M2 Escalate Privilege | UC4 Manage User Roles | S3 role check, S6 audit (detects) | HTTP 403, no data returned, event logged |
| M3 Inject SQL | UC3 Transfer Funds | S4 validation and parameterized queries | Request refused before any query runs |
| M4 Hijack Session | UC2 View Account Balance | S5 TLS 1.3 | Connection closed, nothing sent in clear |

## Render

```
java -jar plantuml.jar -tpng secure_account_portal_usecase.puml
java -jar plantuml.jar -tsvg secure_account_portal_usecase.puml
```

PlantUML 1.2024.7 was used (https://github.com/plantuml/plantuml/releases). Graphviz must be installed. GitHub Actions renders the diagram on every push (`.github/workflows/build.yml`).
