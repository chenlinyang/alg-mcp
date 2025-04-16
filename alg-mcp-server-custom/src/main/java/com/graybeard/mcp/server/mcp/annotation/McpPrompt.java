package com.graybeard.mcp.server.mcp.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <h1>标记为一个MCP Prompt方法</h1>
 *
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface McpPrompt {
    /**
     * <h2>方法名</h2>
     */
    String name() default "";
    /**
     * <h2>描述</h2>
     */
    String description() default "";
}
