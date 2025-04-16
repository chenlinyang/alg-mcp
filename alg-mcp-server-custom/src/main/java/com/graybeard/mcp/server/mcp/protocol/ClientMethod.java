package com.graybeard.mcp.server.mcp.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <h1>ClientMethod</h1>
 *
 * @author C.LY
 */
@Getter
@AllArgsConstructor
public enum ClientMethod {
    /**
     * <h3>初始化</h3>
     */
    INITIALIZE( "initialize"),

    /**
     * <h3>工具调用</h3>
     */
    TOOLS_CALL("tools/call"),

    /**
     * <h3>工具列表</h3>
     */
    TOOLS_LIST("tools/list"),

    /**
     * <h3>通知</h3>
     */
    NOTIFICATIONS_INITIALIZED("notifications/initialized"),
    NOTIFICATION_CANCELLED("notifications/cancelled"),

    /**
     * <h3>ping</h3>
     */
    PING( "ping"),

    /**
     * <h3>资源</h3>
     */
    RESOURCES_LIST("resources/list"),
    RESOURCES_READ("resources/read"),
    RESOURCES_TEMPLATES_LIST("resources/templates/list"),

    /**
     * <h3>提示词</h3>
     */
    PROMPTS_LIST(("prompts/list")),
    PROMPTS_GET("prompts/get")
    ;

    private final String label;
}
