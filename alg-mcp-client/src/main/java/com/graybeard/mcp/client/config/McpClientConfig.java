package com.graybeard.mcp.client.config;

import com.graybeard.mcp.client.service.WeatherAssistant;
import com.graybeard.mcp.client.service.DatabaseAssistant;
import com.graybeard.mcp.client.rag.DefaultContentRetriever;
import com.graybeard.mcp.client.rag.McpDatabaseQueryTransformer;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * McpClientConfig
 *
 * @author C.LY
 * @since 2025-04-12
 */
@Configuration
public class McpClientConfig {
    @Value("${mcp.server.sse-url}")
    private String sseUrl;

    @Value("${zhipu-ai.api-key}")
    private String apiKey;

    @Value("${zhipu-ai.model}")
    private String model;

    /**
     * 初始化ZhipuAiChatModel
     */
    @Bean
    public ZhipuAiChatModel chatModel() {
        return ZhipuAiChatModel.builder()
                .apiKey(apiKey)
                .model(model)
                .temperature(0.7)
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 初始化ZhipuAiStreamingChatModel
     */
    @Bean
    public ZhipuAiStreamingChatModel streamingChatModel() {
        return ZhipuAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .model(model)
                .temperature(0.7)
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 初始化MCP Stdio Client
     */
    //@Bean
    @Deprecated
    public McpClient mcpStdioClient() {
        return new DefaultMcpClient.Builder()
                .transport(new StdioMcpTransport.Builder()
                        .command(List.of(
                                "java",
                                "-Dspring.ai.mcp.server.stdio=true",
                                "-jar",
                                "alg-mcp-server-spring-0.0.1-SNAPSHOT.jar",
                                "--weather.api.host=%s",
                                "--weather.api.api-key=%s"
                                        .formatted(System.getenv("HEFENG_WEATHER_HOST"),
                                                System.getenv("HEFENG_WEATHER_API_KEY"))))
                        .logEvents(true) // only if you want to see the traffic in the log
                        .build())
                .build();
    }

    /**
     * 初始化MCP SSE Client
     */
    @Bean
    public McpClient mcpSseClient() {
        return new DefaultMcpClient.Builder()
                .transport(new HttpMcpTransport.Builder()
                        .sseUrl(sseUrl)
                        .timeout(Duration.ofSeconds(60))
                        .logRequests(true)
                        .logResponses(true)
                        .build())
                .build();
    }

    /**
     * 初始化AiAssistant
     */
    @Bean
    public WeatherAssistant weatherAssistant(McpClient mcpSseClient) {
        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpSseClient))
                .build();

        return AiServices.builder(WeatherAssistant.class)
                .chatLanguageModel(chatModel())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .toolProvider(toolProvider)
                .build();
    }


    /**
     * 初始化DatabaseAssistant
     */
    @Bean
    public DatabaseAssistant databaseAssistant(McpClient mcpSseClient) {
        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpSseClient))
                .build();

        DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                // 调用 Mcp 提示词，自然语言->SQL
                .queryTransformer(new McpDatabaseQueryTransformer(chatModel(), mcpSseClient))
                // 查询路由
                .queryRouter(new DefaultQueryRouter(new DefaultContentRetriever()))
                // 内容检索器
                .contentRetriever(new DefaultContentRetriever())
                // RRF算法
                .contentAggregator(new DefaultContentAggregator())
                .build();

        return AiServices.builder(DatabaseAssistant.class)
                .chatLanguageModel(chatModel())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .retrievalAugmentor(retrievalAugmentor)
                .toolProvider(toolProvider)
                .build();
    }

    // local tools
    /*@Bean
    public WeatherAssistant weatherAssistant() {
        return AiServices.builder(WeatherAssistant.class)
                .streamingChatLanguageModel(streamingChatModel())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .tools(new WeatherTools())
                .build();
    }*/
}
