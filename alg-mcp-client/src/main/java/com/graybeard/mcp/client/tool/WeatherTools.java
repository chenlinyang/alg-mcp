package com.graybeard.mcp.client.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * WeatherTools
 *
 * @author C.LY
 * @since 2025-04-12
 */
public class WeatherTools {
    @Tool("Returns the weather forecast for tomorrow for a given city")
    String getWeather(@P("The city for which the weather forecast should be returned") String city) {
        return "The weather tomorrow in " + city + " is 25°C";
    }
}
