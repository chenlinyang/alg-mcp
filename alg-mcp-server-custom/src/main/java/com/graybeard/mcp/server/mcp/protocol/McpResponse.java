package com.graybeard.mcp.server.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <h1>MCP响应</h1>
 *
 * @author C.LY
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class McpResponse extends McpJson {
    /**
     * <h3>结果</h3>
     */
    private Object result;

    /**
     * <h3>错误</h3>
     */
    private McpError error;
}
