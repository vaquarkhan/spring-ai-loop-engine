# GitHub Actions

CI for Spring AI Loop Engine.

| Workflow | Purpose |
|----------|---------|
| [build.yml](build.yml) | JDK 21 + `mvn verify` on push/PR to `main` |

```bash
mvn -B verify
```

Requires JDK 21+.

The repository home page README is the root [`/README.md`](../README.md) (not this folder).
