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

## Direction

The long-term goal of Kroll is to support safe feature rollouts and live game control, including:

- Kill switches and fast rollback
- Gradual rollouts and targeting
- Scheduled activations and live events
- Environment promotion and change history

These capabilities build directly on the existing APIs and data model.

---

## License & project intent

Kroll is released under the **Apache License, Version 2.0**.

This means it is fully open source, permissive for both open and commercial use, and includes an explicit patent grant.

Kroll is built to be useful first: easy to adopt, easy to integrate, and safe to use in both open and proprietary environments. While the license does not require sharing changes, contributions are encouraged. If you extend Kroll or build something on top of it, consider contributing improvements upstream or sharing feedback.

Commercial use is explicitly allowed. If you are interested in paid support, consulting, or potential hosted offerings in the future, feel free to reach out.

---

## License

Apache 2.0
