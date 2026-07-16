# docs

Project documentation for Spring AI Loop Engine.

## Start here (developers)

| Document | Description |
|----------|-------------|
| **[Developer Guide](developer-guide.md)** | Full reference: install, `spring.ai.loop.*` properties, APIs, modules, production checklist, limitations |
| **[Tutorial](tutorial.md)** | Hands-on labs 0–14: demo → ChatClient → tools → AG-UI → Bastion → integrity → OTel → A2A → HITL |

## Demo (managers & GitHub visitors)

| Document | Description |
|----------|-------------|
| [demo-preview.html](demo-preview.html) | Offline simulation UI (no Spring Boot required) |
| [../examples/simple-loop-app/README.md](../examples/simple-loop-app/README.md) | Live demo: build/start commands + scenarios |

The interactive demo is **not running on GitHub**. Build and start locally for live results; use simulation or `demo-preview.html` otherwise.

| Mode | How | What you get |
|------|-----|----------------|
| **Live** | `mvn -pl examples/simple-loop-app spring-boot:run` → http://localhost:8080/ | Real Loop Engine |
| **Simulation** | Server down, checkbox on the page, or open `demo-preview.html` | Sample outcomes |

## Other

| Document | Description |
|----------|-------------|
| [cursor-setup.md](cursor-setup.md) | Optional MCP editor wiring notes |
| [proposal-update.md](proposal-update.md) | Draft text for Spring AI Community proposal updates |

## Related

- Root overview: [../README.md](../README.md)
- Module details: each `spring-ai-loop-engine-*/README.md`
- Contributing: [../CONTRIBUTING.md](../CONTRIBUTING.md)
- Community proposal: [spring-ai-community/community#28](https://github.com/spring-ai-community/community/issues/28)
