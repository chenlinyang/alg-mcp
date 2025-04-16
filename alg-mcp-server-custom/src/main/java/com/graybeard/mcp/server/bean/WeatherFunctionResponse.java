package com.graybeard.mcp.server.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeatherFunctionResponse {

    @JsonProperty(value = "obsTime")
    @JsonPropertyDescription("数据观测时间")
    private String obsTime;

    @JsonProperty(value = "temp")
    @JsonPropertyDescription("温度（摄氏度）")
    private String temp;

    @JsonProperty(value = "feelsLike")
    @JsonPropertyDescription("体感温度（摄氏度）")
    private String feelsLike;

    @JsonProperty(value = "icon")
    @JsonPropertyDescription("天气状况图标代码")
    private String icon;

    @JsonProperty(value = "text")
    @JsonPropertyDescription("天气状况描述")
    private String text;

    @JsonProperty(value = "wind360")
    @JsonPropertyDescription("风向360角度")
    private String wind360;

    @JsonProperty(value = "windDir")
    @JsonPropertyDescription("风向")
    private String windDir;

    @JsonProperty(value = "windScale")
    @JsonPropertyDescription("风力等级")
    private String windScale;

    @JsonProperty(value = "windSpeed")
    @JsonPropertyDescription("风速（公里/小时）")
    private String windSpeed;

    @JsonProperty(value = "humidity")
    @JsonPropertyDescription("相对湿度（%）")
    private String humidity;

    @JsonProperty(value = "precip")
    @JsonPropertyDescription("过去1小时降水量（毫米）")
    private String precip;

    @JsonProperty(value = "pressure")
    @JsonPropertyDescription("大气压强（百帕）")
    private String pressure;

    @JsonProperty(value = "vis")
    @JsonPropertyDescription("能见度（公里）")
    private String vis;

    @JsonProperty(required = false, value = "cloud")
    @JsonPropertyDescription("云量（%），可能为空")
    private String cloud;

    @JsonProperty(required = false, value = "dew")
    @JsonPropertyDescription("露点温度，可能为空")
    private String dew;

}
