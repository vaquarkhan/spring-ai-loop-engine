package io.github.vaquarkhan.loopengine.mcp.cursor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Cursor IDE {@code mcp.json} snippets so developers can expose Java agent loops to Composer.
 */
public class CursorMcpConfigGenerator {

    private final ObjectMapper objectMapper;

    public CursorMcpConfigGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public CursorMcpConfigGenerator() {
        this(new ObjectMapper());
    }

    /**
     * Build an mcp.json document for a local Spring AI MCP SSE server.
     */
    public String generateSse(String serverName, String sseUrl) {
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("url", sseUrl);
        return toMcpJson(serverName, server);
    }

    /**
     * Build an mcp.json document for a STDIO-launched MCP process.
     */
    public String generateStdio(String serverName, String command, String... args) {
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("command", command);
        server.put("args", args);
        return toMcpJson(serverName, server);
    }

    public Path writeTo(Path path, String json) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, json);
        return path;
    }

    private String toMcpJson(String serverName, Map<String, Object> serverConfig) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode mcpServers = root.putObject("mcpServers");
            mcpServers.set(serverName, objectMapper.valueToTree(serverConfig));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n";
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to render mcp.json", e);
        }
    }
}
