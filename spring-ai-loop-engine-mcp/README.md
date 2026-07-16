# spring-ai-loop-engine-mcp

MCP governance (Bastion RBAC) and `mcp.json` generation for MCP-capable IDE/clients.

Sits between the agent loop and external tools so the loop only executes tools the invoking human principal is allowed to use. Also generates a portable `mcp.json` snippet so editors that speak MCP can connect to your backend tools.

## Artifact

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>spring-ai-loop-engine-mcp</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Package layout

| Package / class | Role |
|-----------------|------|
| `bastion.McpBastionToolExecutor` | Decorates `ToolExecutor` with RBAC checks |
| `bastion.ToolPermissionEvaluator` | SPI for allow/deny decisions |
| `cursor.CursorMcpConfigGenerator` | Builds SSE/STDIO `mcp.json` documents |
| `McpAutoConfiguration` | Bastion bean post-processor + optional config writer |

> Note: the generator package name is historical; the JSON it produces is IDE-agnostic MCP config.

## Bastion usage

```java
ToolPermissionEvaluator evaluator = ToolPermissionEvaluator.allowList(Map.of(
    "alice", Set.of("echo", "search"),
    "admin", Set.of("*")
));

ToolExecutor secured = new McpBastionToolExecutor(delegate, evaluator, () -> currentUser());
```

Denied calls return a structured `rbac_denied` tool error instead of executing the underlying tool.

## Configuration

| Property | Default |
|----------|---------|
| `spring.ai.loop.mcp.enabled` | `true` |
| `spring.ai.loop.mcp.bastion-enabled` | `true` |
| `spring.ai.loop.mcp.generate-cursor-config` | `true` |
| `spring.ai.loop.mcp.cursor-config-path` | `.cursor/mcp.json` |

When `generate-cursor-config` is true, startup writes an `mcp.json` pointing at `http://localhost:8080/sse` (override path/URL as needed for your environment).

## Example mcp.json

```json
{
  "mcpServers": {
    "spring-ai-loop-engine": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Paste into any MCP-capable client (Cursor, Claude Desktop, Gemini tooling, Kiro, etc.).

## Tests

```bash
mvn -pl spring-ai-loop-engine-mcp test
```
