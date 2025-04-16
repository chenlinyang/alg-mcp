package com.graybeard.mcp.server.mcp.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(McpScannerRegistrar.class)
public @interface McpScan {
    String[] value() default {};
    String[] basePackages() default {};
}