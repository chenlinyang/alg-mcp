package com.graybeard.mcp.server.mcp.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>MCP调用方法响应</h1>
 *
 * @author C.LY
 */
@Data
@Accessors(chain = true)
public class McpCallToolResponse {
    /**
     * <h3>内容</h3>
     */
    private List<McpContent> content = new ArrayList<>();
}
