package com.graybeard.mcp.server.mcp.annotation;

import java.lang.annotation.*;

/**
 * <h1>标记为一个MCP方法的参数</h1>
 *
 */
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpParam {
    boolean required() default true;

    String description() default "";
}
