/**
 * Copyright (c) 2017-2024, 湖南智慧政务区块链有限公司.
 */
package com.graybeard.mcp.server.mcp.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * McpContent
 *
 * @author C.LY
 * @since 2025-04-16
 */
@Data
@Accessors(chain = true)
public class McpContent {
    /**
     * <h3>文本</h3>
     */
    private String text;

    /**
     * <h3>类型</h3>
     */
    private String type = "text";
}
