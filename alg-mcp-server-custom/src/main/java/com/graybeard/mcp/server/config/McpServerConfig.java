package com.graybeard.mcp.server.config;

import com.graybeard.mcp.server.mcp.McpService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * McpServerConfig
 *
 * @author C.LY
 * @since 2025-04-16
 */
@Configuration
public class McpServerConfig {

    @Bean
    public McpService mcpService() {
        return new McpService();
    }
}
