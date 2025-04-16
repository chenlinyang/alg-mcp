package com.graybeard.mcp.client;

import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpPrompt;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class McpPromptTest {
    @Resource
    private McpClient mcpSseClient;

    @Test
    public void listPrompt() throws Exception {
        List<McpPrompt> prompts = mcpSseClient.listPrompts();
        assertThat(prompts.size()).isEqualTo(2);
    }
}
