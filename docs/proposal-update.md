# Proposal update draft for spring-ai-community/community#28

Use this text to update Issue #28 and leave consolidation comments on #26 / #30.

## Suggested issue title

`[Proposal] spring-ai-loop-engine — Loop Engineering for Spring AI (AgentLoopManager + Protocol Triad)`

## Body additions

### Project Name & Vision

**Name:** `spring-ai-loop-engine` (follows `spring-ai-[key-feature]`)

**Vision:** Introduce the **Loop Engineering** paradigm to Spring AI. Replace reliance on default recursive `ToolCallingAdvisor` chains with a dedicated, stateful `AgentLoopManager` that:

- Tracks per-turn state (`AgentTurn`)
- Fingerprints tool arguments to prevent duplicate failed retries
- Enforces soft-max-rounds (wrap-up prompt) and hard-max-rounds (structured stop / no runaway billing)

Prompting is abstracted, not eliminated: developers act as loop architects.

### Protocol Triad

1. **AG-UI** — WebFlux SSE streaming (`RunStartedEvent`, `TOOL_CALL_START`, `STATE_DELTA`) + HITL suspension/resume  
2. **A2A** — Integrate with `spring-ai-a2a` patterns; spawn budgeted sub-agents; expose `/.well-known/agent-card.json`  
3. **MCP** — MCP Bastion for zero-trust RBAC between the loop and external tools; native Cursor MCP exposure  

### Cursor developer experience

- Expose backend tools as an MCP server (STDIO/SSE) for Cursor Composer  
- Auto-generate `mcp.json`  
- Ship `.cursor/rules/` templates + `mcp.json` + `docs/cursor-setup.md` + `scripts/install.sh --tool cursor`  

### Consolidation note (comment on #28, reference #26 and #30)

> Consolidating goals from Issue #26 (AgentCore Observability) and Issue #30 (Spring AI Integrity) into this Loop Engine proposal. Native OpenTelemetry (GenAI semantic conventions + PII masking) and PVDM decision attestation are core to running *safe* loops; packaging them together makes the framework enterprise-ready rather than a thin Strands port.

### Repository

https://github.com/vaquarkhan/spring-ai-loop-engine
