# Contributing to Spring AI Loop Engine

Thank you for contributing. This project targets Spring AI Community incubation.

## Principles

1. Stay compatible with Spring AI APIs (`ChatClient`, `ToolCallback`, Advisors).
2. Prefer auto-configuration and property-driven defaults.
3. Soft/hard loop bounds are non-negotiable for any new execution path.
4. Apache 2.0 only — no additional CLA for this pre-incubation repo beyond GitHub ToS.

## Development

```bash
mvn -q verify
```

Java 21+ required.

## Pull requests

- Keep PRs focused (one module / concern when possible)
- Include tests for loop bounds, fingerprinting, Bastion denials, and attestation verify
- Update docs when changing `spring.ai.loop.*` properties

## Code of conduct

Follow the [Spring Code of Conduct](https://github.com/spring-projects/.github/blob/main/CODE_OF_CONDUCT.md).
