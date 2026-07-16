# examples

Runnable sample applications that demonstrate Spring AI Loop Engine.

## Samples

| Sample | Description |
|--------|-------------|
| [simple-loop-app](simple-loop-app/README.md) | Minimal WebFlux app with a demo model/tool client — no API key required |

## Run

From the repository root (JDK 21+):

```bash
mvn -pl examples/simple-loop-app -am install -DskipTests
mvn -pl examples/simple-loop-app spring-boot:run
```

Then:

```bash
curl -X POST http://localhost:8080/api/loop/run \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"demo\"}"
```

More samples can be added here as the framework grows (HITL UI demos, A2A workers, Bastion RBAC demos).

Developer docs: [Developer Guide](../docs/developer-guide.md) · [Tutorial](../docs/tutorial.md).
