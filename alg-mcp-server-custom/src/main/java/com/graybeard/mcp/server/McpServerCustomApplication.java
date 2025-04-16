package com.graybeard.mcp.server;

import com.graybeard.mcp.server.mcp.annotation.McpScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@McpScan(basePackages = "com.graybeard.mcp.server")
public class McpServerCustomApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerCustomApplication.class, args);
    }
}
