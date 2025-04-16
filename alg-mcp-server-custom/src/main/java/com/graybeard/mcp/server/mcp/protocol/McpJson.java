package com.graybeard.mcp.server.mcp.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <h1>MCP JSON-RPC 2.0</h1>
 *
 * @author @author C.LY
 */
@Data
@Accessors(chain = true)
public class McpJson {
    /**
     * <h3>JSONRPC版本</h3>
     */
    private String jsonrpc = "2.0";

    /**
     * <h3>ID</h3>
     */
    private Long id = 0L;
}
