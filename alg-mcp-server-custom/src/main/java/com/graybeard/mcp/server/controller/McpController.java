package com.graybeard.mcp.server.controller;

import com.graybeard.mcp.server.mcp.McpService;
import com.graybeard.mcp.server.mcp.protocol.McpErrorCode;
import com.graybeard.mcp.server.mcp.exception.McpException;
import com.graybeard.mcp.server.mcp.protocol.ClientMethod;
import com.graybeard.mcp.server.mcp.protocol.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <h1>MCP 控制器</h1>
 *
 * @author C.LY
 */
@Slf4j
@RestController
public class McpController {
    public static final ConcurrentMap<String, String> SESSION_PERSONAL_TOKENS = new ConcurrentHashMap<>();

    @Resource
    private McpService mcpService;


    /**
     * {
     *   "jsonrpc": "2.0",
     *   "id": "string | number",
     *   "method": "string",
     *   "param?": {
     *     "key": "value"
     *   }
     * }
     *
     * @return
     * @throws IOException
     * @throws McpException
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() throws IOException, McpException {
        String uuid = UUID.randomUUID().toString();
        SseEmitter sseEmitter = McpService.getSseEmitter(uuid);

        sseEmitter.send(SseEmitter.event()
                .name("endpoint")
                .data("/mcp/messages?sessionId=" + uuid)
        );

        log.info("sse connect: " + uuid);
        return sseEmitter;
    }

    @PostMapping("/mcp/messages")
    public void messages(HttpServletRequest request, @RequestBody McpRequest mcpRequest) {
        String uuid = request.getParameter("sessionId");

        if(!McpService.EMITTERS.containsKey(uuid)) {
            return;
        }
        String method = mcpRequest.getMethod();

        McpResponse mcpResponse = null;
        try {
            if (!StringUtils.hasText(method)) {
                McpService.emitError(uuid, mcpRequest.getId(), McpErrorCode.MethodNotFound);
                return;
            }
            ClientMethod mcpMethods = Arrays.stream(ClientMethod.values())
                    .filter(item -> item.getLabel().equals(method))
                    .findFirst()
                    .orElse(null);
            if (Objects.isNull(mcpMethods)) {
                McpService.emitError(uuid, mcpRequest.getId(), McpErrorCode.MethodNotFound);
                return;
            }
            mcpResponse = mcpService.run(uuid, mcpMethods, mcpRequest);
            log.info("McpResponse: " + mcpResponse);
        } catch (McpException mcpException) {
            McpService.emitResult(uuid, mcpRequest.getId(), mcpException.getMessage());
        }
    }
}
