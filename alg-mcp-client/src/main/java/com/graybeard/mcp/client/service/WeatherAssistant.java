package com.graybeard.mcp.client.service;

import reactor.core.publisher.Flux;

/**
 * 天气助手
 *
 * @author C.LY
 * @since 2025-04-12
 */
public interface WeatherAssistant {
    String chat(String message);

    Flux<String> streamChat(String message);
}
