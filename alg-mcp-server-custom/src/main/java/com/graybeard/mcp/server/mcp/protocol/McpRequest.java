package com.graybeard.mcp.server.mcp.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <h1>MCP请求</h1>
 *
 * @author C.LY
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class McpRequest extends McpJson {
    /**
     * <h3>请求参数</h3>
     */
    private Object params;

    /**
     * <h3>请求方法</h3>
     */
    private String method;

    private McpClientInfo clientInfo;
}
