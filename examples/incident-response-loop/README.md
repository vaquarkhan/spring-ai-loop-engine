# Incident Response Loop

**Real-world use case:** SRE investigates a SEV-2 (alerts → logs → SLO), then attempts remediation. **MCP Bastion** allows `restart_service` only for actor `sre-oncall`.

No LLM API key required.

## Business story

| Step | Tool | Who |
|------|------|-----|
| 1 | `get_alerts` | any |
| 2 | `query_logs` | any |
| 3 | `check_slo` | any |
| 4 | `restart_service` | **sre-oncall only** (else `rbac_denied`) |

Request body field `actor` stands in for an IdP / gateway principal (demo ThreadLocal → Bastion evaluator).

## Run

```bash
mvn -pl examples/incident-response-loop -am install -DskipTests
mvn -pl examples/incident-response-loop spring-boot:run
```

App listens on **http://localhost:8083/**

## Try it

```bash
# Diagnosis only (no restart)
curl -s -X POST http://localhost:8083/api/incident \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"diagnose only checkout latency\",\"actor\":\"analyst\"}"

# Analyst tries remediation — Bastion denies restart
curl -s -X POST http://localhost:8083/api/incident \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"remediate restart checkout\",\"actor\":\"analyst\"}"

# On-call may restart
curl -s -X POST http://localhost:8083/api/incident \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"remediate restart checkout\",\"actor\":\"sre-oncall\"}"
```

## Loop Engine features shown

- Multi-tool incident loop
- Soft / hard budgets
- MCP Bastion RBAC on a destructive tool
- Safe wrap-up when remediation is denied

See the [Tutorial](../../docs/tutorial.md) Lab 8 (Bastion).
