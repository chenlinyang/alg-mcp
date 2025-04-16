package com.graybeard.mcp.server.mcp.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <h1>MCP客户端信息</h1>
 *
 * @author C.LY
 */
@Data
@Accessors(chain = true)
public class McpClientInfo {
    /**
     * <h3>客户端信息</h3>
     */
    private String name = "mcp-client";

    /**
     * <h3>服务器版本</h3>
     */
    private String version = "0.0.1";
}
