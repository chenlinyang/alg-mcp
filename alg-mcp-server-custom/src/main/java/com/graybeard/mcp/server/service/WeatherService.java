package com.graybeard.mcp.server.service;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.graybeard.mcp.server.config.WeatherApiProperties;
import com.graybeard.mcp.server.bean.WeatherFunctionResponse;
import com.graybeard.mcp.server.mcp.annotation.McpTool;
import com.graybeard.mcp.server.mcp.annotation.McpParam;
import jakarta.annotation.Resource;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WeatherService {

    @Resource
    private WeatherApiProperties weatherApiProperties;

    @McpTool(description = "获取某个城市的实时天气")
    public WeatherFunctionResponse getWeatherByCity(@McpParam(description = "要查询的城市名称") String city) {
//        String city = request.getCity();
        log.info("开始获取天气，城市：{}",city);
        // 先调用城市搜索接口，查询到该城市的locationId
        String citySearchApiUrl = weatherApiProperties.getHost() + "/geo/v2/city/lookup";
        @Cleanup
        HttpResponse citySearchHttpResponse = HttpUtil.createGet(citySearchApiUrl)
                .header("Content-Type", "application/json")
                .header("X-QW-Api-Key", weatherApiProperties.getApiKey())
                .form("location", city)
                .execute();

        String cityResponseBody = citySearchHttpResponse.body();
        log.info("城市搜索接口返回结果：{}", cityResponseBody);
        String locationId = JSONUtil.getByPath(JSONUtil.parseObj(cityResponseBody), "$.location[0].id", null);

        log.info("城市的locationId为：{}", locationId);
        // 再调用天气接口，获取天气信息
        @Cleanup
        HttpResponse weatherHttpResponse = HttpUtil.createGet(weatherApiProperties.getHost() + "/v7/weather/now")
                .header("Content-Type", "application/json")
                .header("X-QW-Api-Key", weatherApiProperties.getApiKey())
                .form("location", locationId)
                .form("lang", "zh")
                .execute();
        String body = weatherHttpResponse.body();
        log.info("天气接口返回结果：{}", body);
        JSONObject jsonObject = JSONUtil.parseObj(body);
        return jsonObject.getJSONObject("now").toBean(WeatherFunctionResponse.class);
    }


}
