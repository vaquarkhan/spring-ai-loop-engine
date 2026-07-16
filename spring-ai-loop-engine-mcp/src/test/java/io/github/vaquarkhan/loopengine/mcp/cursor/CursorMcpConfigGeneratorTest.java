package io.github.vaquarkhan.loopengine.mcp.cursor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CursorMcpConfigGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generateSseAndStdioAndWriteFile() throws Exception {
        CursorMcpConfigGenerator generator = new CursorMcpConfigGenerator();

        String sse = generator.generateSse("spring-ai-loop-engine", "http://localhost:8080/sse");
        assertThat(sse).contains("\"mcpServers\"");
        assertThat(sse).contains("http://localhost:8080/sse");

        String stdio = generator.generateStdio("local-tools", "java", "-jar", "tools.jar");
        assertThat(stdio).contains("\"command\" : \"java\"");
        assertThat(stdio).contains("tools.jar");

        Path out = tempDir.resolve("mcp.json");
        generator.writeTo(out, sse);
        assertThat(Files.readString(out)).isEqualTo(sse);
    }
}
