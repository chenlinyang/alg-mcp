package com.graybeard.mcp.client.service;

import reactor.core.publisher.Flux;

/**
 * Database助手
 *
 * @author C.LY
 * @since 2025-04-12
 */
public interface DatabaseAssistant {

    String chat(String message);

    Flux<String> streamChat(String message);
}
