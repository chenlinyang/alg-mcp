package com.graybeard.mcp.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "weather.api")
@Data
@Component
public class WeatherApiProperties {
    private String host;
    private String apiKey;
}
