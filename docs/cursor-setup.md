# Cursor IDE setup for Spring AI Loop Engine

This guide wires the Loop Engine into [Cursor](https://cursor.com) so Composer can call your Java agent tools over MCP and inherit project rules on every chat.

## 1. Install helpers

```bash
# macOS / Linux
./scripts/install.sh --tool cursor

# Windows
./scripts/install.ps1 -Tool cursor
```

This copies:

- `.cursor/rules/loop-engine.mdc` — project rules for loop architecture (always applied)
- `.cursor/mcp.json` — MCP server snippet for Cursor Settings

## 2. MCP server exposure

Expose your Spring Boot agent as an MCP server (STDIO or SSE) using Spring AI MCP annotations (`@McpTool`, etc.). Point Cursor at that server:

```json
{
  "mcpServers": {
    "spring-ai-loop-engine": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Or STDIO:

```json
{
  "mcpServers": {
    "spring-ai-loop-engine": {
      "command": "java",
      "args": ["-jar", "target/your-mcp-server.jar"]
    }
  }
}
```

Generate programmatically:

```java
@Autowired CursorMcpConfigGenerator generator;

String json = generator.generateSse("spring-ai-loop-engine", "http://localhost:8080/sse");
generator.writeTo(Path.of(".cursor/mcp.json"), json);
```

With `spring.ai.loop.mcp.generate-cursor-config=true` (default), the starter writes `.cursor/mcp.json` on startup.

## 3. What Composer can do

Once MCP is connected, Cursor’s agent can invoke your backend tools — PII redactors, PVDM validation gates, loop run helpers — with the same MCP Bastion RBAC that protects production tool calls.

## 4. Loop architect mindset (injected via `.cursor/rules/`)

- Prefer `AgentLoopManager` over hand-rolled recursive advisors  
- Set soft/hard round budgets for every long-running loop  
- Stream progress with AG-UI SSE for UIs  
- Never bypass MCP Bastion for sensitive tools  
- Emit PVDM attestations for durable audit trails  

## 5. Verify

1. Start the example: `mvn -pl examples/simple-loop-app -am spring-boot:run`  
2. Confirm `.cursor/mcp.json` exists  
3. In Cursor → Settings → MCP, ensure `spring-ai-loop-engine` is green  
4. Ask Composer: “Run a loop turn that echoes hello via the demo tool”
