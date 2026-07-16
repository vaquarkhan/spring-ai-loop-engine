# Support Triage Loop

**Real-world use case:** Customer support agent loop — load ticket + customer, search knowledge base, draft a reply or escalate; billing disputes can spawn a budgeted A2A specialist.

No LLM API key required.

## Business story

| Step | Tool | Outcome |
|------|------|---------|
| 1 | `get_ticket` | TCK-5521 duplicate charge |
| 2 | `get_customer` | VIP Business customer |
| 3 | `search_kb` | KB-12 refund policy |
| 4a | `draft_reply` | Ready for human review |
| 4b | `escalate_l2` | BILLING_L2 queue |
| A2A | worker soft=3 / hard=5 | Focused billing specialist |

## Run

```bash
mvn -pl examples/support-triage-loop -am install -DskipTests
mvn -pl examples/support-triage-loop spring-boot:run
```

App listens on **http://localhost:8082/**

## Try it

```bash
# Standard triage + draft
curl -s -X POST http://localhost:8082/api/triage \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"billing charge refund triage\"}"

# Escalate VIP dispute
curl -s -X POST http://localhost:8082/api/triage \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"escalate VIP billing dispute\"}"

# A2A billing specialist (tight budget)
curl -s -X POST http://localhost:8082/api/triage/billing-specialist \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"billing charge refund triage\"}"
```

## Loop Engine features shown

- Multi-tool CX workflow
- Soft / hard budgets
- A2A `SubAgentSpawner` with worker round caps
- Human-in-the-loop *policy* in the final answer (draft/escalate — no auto-refund)

See the [Tutorial](../../docs/tutorial.md).
