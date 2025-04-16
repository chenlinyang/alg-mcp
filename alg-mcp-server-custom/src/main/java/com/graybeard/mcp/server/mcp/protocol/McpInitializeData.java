package com.graybeard.mcp.server.mcp.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <h1>MCP初始化数据</h1>
 *
 * @author C.LY
 */
@Data
@Accessors(chain = true)
public class McpInitializeData {
    /**
     * <h3>服务器信息</h3>
     */
    private McpServerInfo serverInfo = new McpServerInfo();

    /**
     * <h3>协议版本</h3>
     */
    private String protocolVersion = "2024-11-05";

    /**
     * <h3>能力</h3>
     */
    private Capabilities capabilities = new Capabilities();

    @Data
    @Accessors(chain = true)
    public static class Capabilities {
        /**
         * <h3>工具</h3>
         */
        private Tools tools = new Tools();
    }

    @Data
    @Accessors(chain = true)
    public static class Tools {
        private boolean listChanged = true;
    }
}
