# Kroll

**Kroll is a self-hosted remote configuration and feature-flag service for games.**

It provides a backend and API for defining feature flags and configuration values that game clients can fetch at runtime, without requiring a new game patch.

Kroll is designed for game development workflows, where patching is slow, live issues are costly, and teams need safe, reversible control over game behavior.

---

## Current status

Kroll is early-stage and currently supports:

- CRUD APIs for feature flags and configuration entries
- Projects and environments as first-class concepts
- JWT authentication for administrators
- API key authentication for game clients
- HTTP API for client-side flag evaluation
---

## Development

```bash
make dev
```
will run the infrastructure required by the app with compose and then launch
bootRun locally in dev mode. This is the standard development setup.

## Kroll UI

[KRoll UI](https://github.com/Collederas/K-Roll-UI) is a sample supported UI that integrates with KRoll.
The following command will launch a reverse proxy with a demo setup.

```bash
make ui
```
You can now reach the app at localhost:8080.
In dev mode a test user is seeded with following credentials:

```
username: test
password: password123
```

To stop all services:

```bash
make down
```
---
## Direction

The long-term goal of Kroll is to support safe feature rollouts and live game control, including:

- Kill switches and fast rollback
- Gradual rollouts and targeting
- Scheduled activations and live events
- Environment promotion and change history

---

## License & project intent

Kroll is released under the **Apache License, Version 2.0**.

This means it is fully open source, permissive for both open and commercial use, and includes an explicit patent grant.

Kroll is built to be useful first: easy to adopt, easy to integrate, and safe to use in both open and proprietary environments. While the license does not require sharing changes, contributions are encouraged. If you extend Kroll or build something on top of it, consider contributing improvements upstream or sharing feedback.

---

## License

Apache 2.0
