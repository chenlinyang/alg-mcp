package com.graybeard.mcp.server.mcp.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h1>MCP错误码</h1>
 *
 * @author C.LY
 */
@Getter
@AllArgsConstructor
public enum McpErrorCode {
    /**
     * <h3>转换错误</h3>
     */
    ParseError(-32700, "Parse error"),

    /**
     * <h3>请求错误</h3>
     */
    InvalidRequest(-32600, "Invalid Request"),

    /**
     * <h3>方法未找到</h3>
     */
    MethodNotFound(-32601, "Method not found"),

    /**
     * <h3>参数错误</h3>
     */
    InvalidParams(-32602, "Invalid params"),

    /**
     * <h3>内部错误</h3>
     */
    InternalError(-32603, "Internal error");

    private final int key;
    private final String label;
}
