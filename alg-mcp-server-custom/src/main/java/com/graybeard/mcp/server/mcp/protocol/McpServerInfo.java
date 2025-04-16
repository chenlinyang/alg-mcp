package com.graybeard.mcp.server.mcp.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <h1>MCP服务器信息</h1>
 *
 * @author C.LY
 */
@Data
@Accessors(chain = true)
public class McpServerInfo {
    /**
     * <h3>服务器名称</h3>
     */
    private String name = "mcp-server";

    /**
     * <h3>服务器版本</h3>
     */
    private String version = "0.0.1";
}
