package com.graybeard.mcp.server.mcp.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <h1>MCP Error</h1>
 *
 * @author C.LY
 */
@Data
@Accessors(chain = true)
public class McpError {
    public McpError() {
    }

    public McpError(McpErrorCode errorCode) {
        this.code = errorCode.getKey();
        this.message = errorCode.getLabel();
    }
    /**
     * <h3>错误代码</h3>
     */
    private Integer code;

    /**
     * <h3>错误信息</h3>
     */
    private String message;
}
