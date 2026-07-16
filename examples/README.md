# examples

Runnable sample applications that demonstrate Spring AI Loop Engine on **real-world use cases**.

None of these demos require an LLM API key (scenario-driven `LoopModelClient` + tools).

## Samples

| Sample | Port | Domain | What it shows |
|--------|------|--------|----------------|
| [simple-loop-app](simple-loop-app/README.md) | 8080 | Getting started | Manager UI, simulation fallback, multi-tool / soft wrap / fingerprint / A2A |
| [invoice-reconciliation-loop](invoice-reconciliation-loop/README.md) | 8081 | Finance / AP | Match payment ↔ invoice, open exceptions, duplicate ERP retry blocked |
| [support-triage-loop](support-triage-loop/README.md) | 8082 | Customer support | Ticket → customer → KB → draft/escalate; A2A billing specialist |
| [incident-response-loop](incident-response-loop/README.md) | 8083 | SRE / DevOps | Alerts → logs → SLO; Bastion RBAC on `restart_service` |

## Run one example

```bash
# from repo root (JDK 21+)
mvn -pl examples/<artifactId> -am install -DskipTests
mvn -pl examples/<artifactId> spring-boot:run
```

Examples:

```bash
mvn -pl examples/invoice-reconciliation-loop -am spring-boot:run
mvn -pl examples/support-triage-loop -am spring-boot:run
mvn -pl examples/incident-response-loop -am spring-boot:run
```

Each app uses a **different port** so you can run more than one at once.

## Quick curls

```bash
# Finance
curl -s -X POST http://localhost:8081/api/reconcile -H "Content-Type: application/json" \
  -d "{\"message\":\"reconcile INV-1042 mismatch short pay\"}"

# Support + A2A worker
curl -s -X POST http://localhost:8082/api/triage/billing-specialist -H "Content-Type: application/json" \
  -d "{\"message\":\"billing charge refund triage\"}"

# SRE — RBAC deny vs on-call
curl -s -X POST http://localhost:8083/api/incident -H "Content-Type: application/json" \
  -d "{\"message\":\"remediate restart checkout\",\"actor\":\"analyst\"}"
curl -s -X POST http://localhost:8083/api/incident -H "Content-Type: application/json" \
  -d "{\"message\":\"remediate restart checkout\",\"actor\":\"sre-oncall\"}"
```

## Docs

- [Developer Guide](../docs/developer-guide.md)
- [Tutorial](../docs/tutorial.md)
