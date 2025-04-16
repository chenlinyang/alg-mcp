package com.graybeard.mcp.server.mcp.protocol;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * <h1>McpPromptDefinition</h1>
 *
 * @author C.LY
 */
@Slf4j
@Data
@Accessors(chain = true)
public class McpPromptDefinition {
    /**
     * <h3>工具名称</h3>
     */
    private String name;

    /**
     * <h3>工具描述</h3>
     */
    private String description;

    /**
     * <h3>输入参数</h3>
     */
    private List<Argument> arguments;

    /**
     * <h3>输入参数</h3>
     *
     * @author Hamm.cn
     */
    @Data
    @Accessors(chain = true)
    public static class Argument {
        /**
         * <h3>名称</h3>
         */
        private String name;

        /**
         * <h3>描述</h3>
         */
        private String description;

        private boolean required;

    }
}
