package com.graybeard.mcp.server.mcp.exception;

/**
 * <h1>MCP异常</h1>
 *
 * @author C.LY
 */
public class McpException extends RuntimeException {
    public McpException(String message) {
        super(message);
    }
    public McpException(String message, Throwable cause) {
        super(message, cause);
    }

}
