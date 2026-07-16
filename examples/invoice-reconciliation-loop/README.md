# Invoice Reconciliation Loop

**Real-world use case:** Accounts payable reconciles bank payments to open invoices — match, exception, or fingerprint-blocked ERP retries.

No LLM API key required (demo model + tools).

## Business story

| Step | Tool | Outcome |
|------|------|---------|
| 1 | `fetch_invoice` | Load INV-1042 (OPEN, $1500) |
| 2 | `fetch_payment` | Bank remittance (full or short) |
| 3a | `post_match` | Happy path — amounts equal |
| 3b | `open_exception` | Underpayment → AP exception queue |
| retry | same failing `open_exception` | Duplicate fingerprint blocked |

## Run

```bash
# from repo root (JDK 21+)
mvn -pl examples/invoice-reconciliation-loop -am install -DskipTests
mvn -pl examples/invoice-reconciliation-loop spring-boot:run
```

App listens on **http://localhost:8081/**

## Try it

```bash
# Happy path — match and post
curl -s -X POST http://localhost:8081/api/reconcile \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"reconcile INV-1042 match\"}"

# Underpayment — open exception
curl -s -X POST http://localhost:8081/api/reconcile \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"reconcile INV-1042 mismatch short pay\"}"

# ERP failure + duplicate retry blocked
curl -s -X POST http://localhost:8081/api/reconcile \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"retry duplicate open_exception\"}"
```

## Loop Engine features shown

- Soft / hard round budgets (`soft=6`, `hard=12`)
- Multi-tool autonomous loop
- Duplicate failed-action fingerprinting
- AG-UI / A2A / integrity modules on classpath (same starter)

See the [Tutorial](../../docs/tutorial.md) and [Developer Guide](../../docs/developer-guide.md).
