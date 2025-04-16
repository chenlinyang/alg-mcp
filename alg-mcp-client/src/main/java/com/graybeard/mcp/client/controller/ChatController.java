package com.graybeard.mcp.client.controller;

import com.graybeard.mcp.client.service.WeatherAssistant;
import com.graybeard.mcp.client.service.DatabaseAssistant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * ChatController
 *
 * @author C.LY
 * @since 2025-04-12
 */
@RestController
public class ChatController {

    private final WeatherAssistant weatherAssistant;
    private final DatabaseAssistant databaseAssistant;

    public ChatController(WeatherAssistant weatherAssistant, DatabaseAssistant databaseAssistant) {
        this.weatherAssistant = weatherAssistant;
        this.databaseAssistant = databaseAssistant;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public String chat(String message) {
        return weatherAssistant.chat(message);
    }

    @GetMapping(value = "/analysis", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public String analysis(String message) {
        return databaseAssistant.chat(message);
    }

}
