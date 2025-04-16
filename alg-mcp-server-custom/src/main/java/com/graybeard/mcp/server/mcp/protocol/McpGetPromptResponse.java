package com.graybeard.mcp.server.mcp.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * PromptMessage
 *
 * @author C.LY
 * @since 2025-04-15
 */
@Data
@AllArgsConstructor
public class McpGetPromptResponse {
    private String role;
    private McpContent content;

    public static McpGetPromptResponse withUserRole(McpContent content) {
        return new McpGetPromptResponse("user", content);
    }

}
