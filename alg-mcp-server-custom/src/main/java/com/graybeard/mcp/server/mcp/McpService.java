package com.graybeard.mcp.server.mcp;

import cn.hutool.crypto.digest.DigestUtil;
import com.graybeard.mcp.server.mcp.annotation.McpPrompt;
import com.graybeard.mcp.server.mcp.annotation.McpTool;
import com.graybeard.mcp.server.mcp.annotation.McpParam;
import com.graybeard.mcp.server.mcp.exception.McpException;
import com.graybeard.mcp.server.mcp.protocol.*;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Contract;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <h1>McpService</h1>
 *
 * @author C.LY
 */
@Slf4j
public class McpService {
    /**
     * <h3>SseEmitters</h3>
     */
    public final static ConcurrentMap<String, SseEmitter> EMITTERS = new ConcurrentHashMap<>();
    /**
     * <h3>方法列表</h3>
     */
    public final static ConcurrentMap<String, Method> METHOD_MAP = new ConcurrentHashMap<>();
    /**
     * <h3>工具列表</h3>
     */
    public static List<McpToolDefinition> tools = new ArrayList<>();

    /**
     * <h3>提示词</h3>
     */
    public final static ConcurrentMap<String, Method> PROMPT_MAP = new ConcurrentHashMap<>();
    /**
     * <h3>提示词列表</h3>
     */
    public static List<McpPromptDefinition> prompts = new ArrayList<>();

    /**
     * <h3>定义调度器</h3>
     */
    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());


    @Resource
    private BeanFactory beanFactory;

    /**
     * <h3>扫描Mcp方法</h3>
     *
     * @param packages 包名
     */
    public static void scanMcpMethod(String @NotNull ... packages) {
        Reflections reflections;
        tools = new ArrayList<>();
        for (String pack : packages) {
            reflections = new Reflections(pack, Scanners.MethodsAnnotated);
            Set<Method> toolMethods = reflections.getMethodsAnnotatedWith(McpTool.class);
            Set<Method> promptMethods = reflections.getMethodsAnnotatedWith(McpPrompt.class);
            toolMethods.forEach(method -> {
                McpToolDefinition toolDefinition = getTool(method);
                if (toolDefinition != null) {
                    tools.add(toolDefinition);
                    METHOD_MAP.put(toolDefinition.getName(), method);
                }
            });

            promptMethods.forEach(method -> {
                McpPromptDefinition promptDefinition  = getPrompt(method);
                if (promptDefinition != null) {
                    prompts.add(promptDefinition);
                    PROMPT_MAP.put(promptDefinition.getName(), method);
                }
            });
        }
        log.info("扫描到 {} 个Mcp tools, {} 个Mcp prompts.", tools.size(), prompts.size());
    }

    /**
     * <h3>获取McpTool</h3>
     *
     * @param method 方法
     * @return McpTool
     */
    private static @Nullable McpToolDefinition getTool(@NotNull Method method) {
        McpTool mcpTool = method.getAnnotation(McpTool.class);

        String name;
        String description;
        if(Objects.nonNull(mcpTool)) {
            name = mcpTool.name();
            description = mcpTool.description();
        } else {
            return null;
        }

        McpToolDefinition toolDefinition = new McpToolDefinition();
        if (!StringUtils.hasText(name)) {
            name = method.getDeclaringClass().getSimpleName() + "_" + method.getName();
        }
        McpToolDefinition.InputSchema inputSchema = new McpToolDefinition.InputSchema();
        // 获取Method的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        IntStream.range(0, parameterTypes.length).forEach(index -> {
            Class<?> parameterType = parameterTypes[index];

            String paramName = method.getParameters()[index].getName();

            // 获取Parameter对象
            Parameter parameter = method.getParameters()[index];
            // 获取参数上的注解
            McpParam mcpParam = parameter.getAnnotation(McpParam.class);
            String paramDesc = "";
            if (Objects.nonNull(mcpParam)) {
                paramDesc = mcpParam.description();
                if (mcpParam.required()) {
                    // 标记必选属性的注解
                    inputSchema.getRequired().add(paramName);
                }
            }

            // 参数的描述
            Map<String, McpToolDefinition.InputSchema.Property> properties = inputSchema.getProperties();

            // 初始化
            McpToolDefinition.InputSchema.Property item = new McpToolDefinition.InputSchema.Property().setDescription(paramDesc);

            // 判断方法的类型是否为 String 数字 布尔
            if (parameterType.equals(String.class)) {
                properties.put(paramName, item.setType("string"));
            } else if (parameterType.equals(Boolean.class) || parameterType.equals(boolean.class)) {
                properties.put(paramName, item.setType("boolean"));
            } else if (Number.class.isAssignableFrom(parameterType)) {
                properties.put(paramName, item.setType("number"));
            }
            inputSchema.setProperties(properties);
        });
        toolDefinition.setName(name)
                .setDescription(description)
                .setInputSchema(inputSchema);
        return toolDefinition;
    }


    private static @Nullable McpPromptDefinition getPrompt(@NotNull Method method) {
        McpPrompt mcpPrompt = method.getAnnotation(McpPrompt.class);

        String name;
        String description;
        if(Objects.nonNull(mcpPrompt)) {
            name = mcpPrompt.name();
            description = mcpPrompt.description();
        } else {
            return null;
        }

        McpPromptDefinition promptDefinition = new McpPromptDefinition();
        if (!StringUtils.hasText(name)) {
            name = method.getDeclaringClass().getSimpleName() + "_" + method.getName();
        }

        List<McpPromptDefinition.Argument> arguments = new ArrayList<>();
        // 获取Method的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        IntStream.range(0, parameterTypes.length).forEach(index -> {
            Class<?> parameterType = parameterTypes[index];

            String paramName = method.getParameters()[index].getName();

            McpPromptDefinition.Argument argument = new McpPromptDefinition.Argument();
            argument.setName(paramName);

            // 获取Parameter对象
            Parameter parameter = method.getParameters()[index];
            // 获取参数上的注解
            McpParam mcpParam = parameter.getAnnotation(McpParam.class);
            String paramDesc = "";
            if (Objects.nonNull(mcpParam)) {
                paramDesc = mcpParam.description();
                if (mcpParam.required()) {
                    // 标记必选属性的注解
                    argument.setRequired(true);
                }
            }
            argument.setDescription(paramDesc);

            arguments.add(argument);
        });
        promptDefinition.setName(name)
                .setDescription(description)
                .setArguments(arguments);
        return promptDefinition;
    }

    /**
     * <h3>获取SseEmitter</h3>
     *
     * @param uuid    uuid
     * @param timeout 超时时间 默认 {@code 毫秒}
     * @return SseEmitter
     */
    public static @NotNull SseEmitter getSseEmitter(String uuid, long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);
        McpService.EMITTERS.put(uuid, emitter);

        // 1. 心跳保活机制
        ScheduledFuture<?> pingTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (Objects.nonNull(EMITTERS.get(uuid))) {
                    // 发送符合SSE规范的ping（推荐使用注释行）
                    emitter.send(SseEmitter.event()
                            .comment("ping")); // 客户端无需处理的消息
//                    .reconnectTime(5 * 1000L));
                }
            } catch (Exception e) {
                log.warn("SSE ping发送失败: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        }, 0, 10, TimeUnit.SECONDS);

        // 2. 资源清理
        emitter.onCompletion(() -> {
            EMITTERS.remove(uuid);
            pingTask.cancel(true); // 清除定时任务
            log.debug("SSE连接正常关闭: {}", uuid);
        });

        emitter.onTimeout(() -> {
            EMITTERS.remove(uuid);
            pingTask.cancel(true);
            log.warn("SSE连接超时: {}", uuid);
        });

        return emitter;
    }

    /**
     * <h3>获取SseEmitter</h3>
     *
     * @param uuid uuid
     * @return SseEmitter
     */
    public static @NotNull SseEmitter getSseEmitter(String uuid) {
        return getSseEmitter(uuid, 0);
    }

    /**
     * <h3>发送结果</h3>
     *
     * @param uuid uuid
     * @param id   id
     * @param data 数据
     * @return McpResponse
     * @throws McpException 异常
     */
    public static McpResponse emitResult(String uuid, Long id, Object data) throws McpException {
        McpResponse response = new McpResponse();
        response.setId(id);
        response.setResult(data);
        return emit(uuid, response);
    }

    /**
     * <h3>发送错误</h3>
     *
     * @param uuid uuid
     * @param id   id
     * @param code 错误代码
     * @throws McpException 异常
     */
    public static void emitError(String uuid, Long id, @NotNull McpErrorCode code) throws McpException {
        emitError(uuid, id, code.getKey(), code.getLabel());
    }

    /**
     * <h3>发送错误</h3>
     *
     * @param uuid    uuid
     * @param id      id
     * @param code    错误代码
     * @param message 错误信息
     * @throws McpException 异常
     */
    public static void emitError(String uuid, Long id, Integer code, String message) throws McpException {
        McpResponse response = new McpResponse();
        response.setId(id);
        McpError error = new McpError();
        error.setCode(code).setMessage(message);
        response.setError(error);
        emit(uuid, response);
        throw new McpException(message);
    }

    /**
     * <h3>发送错误</h3>
     *
     * @param uuid    uuid
     * @param id      id
     * @param message 错误信息
     * @throws McpException 异常
     */
    public static void emitError(String uuid, Long id, String message) throws McpException {
        emitError(uuid, id, McpErrorCode.InternalError.getKey(), message);
    }

    /**
     * <h3>发送</h3>
     *
     * @param uuid     uuid
     * @param response 响应
     * @return McpResponse
     * @throws McpException 异常
     */
    @Contract("_, _ -> param2")
    private static McpResponse emit(String uuid, McpResponse response) throws McpException {
        SseEmitter sseEmitter = EMITTERS.get(uuid);
        String string = JsonUtil.toJson(response);
        if (Objects.nonNull(sseEmitter)) {
            try {
                sseEmitter.send(SseEmitter.event()
                        .name("message")
                        .data(string)
                );
            } catch (IOException e) {
                throw new McpException(e.getMessage());
            }
        }
        return response;
    }

    /**
     * <h3>获取访问指定工具需要的权限</h3>
     *
     * @param mcpTool 工具
     * @return 权限标识
     */
    public static @NotNull String getPermissionIdentity(@NotNull McpToolDefinition mcpTool) {
        return DigestUtil.sha1Hex(mcpTool.getName() + mcpTool.getDescription());
    }

    /**
     * <h3>运行方法</h3>
     *
     * @param uuid         uuid
     * @param clientMethod 方法
     * @param mcpRequest   请求
     * @return 响应
     * @throws McpException 异常
     */
    public McpResponse run(String uuid, @NotNull ClientMethod clientMethod, McpRequest mcpRequest) throws McpException {
        McpResponse responseData;
        switch (clientMethod) {
            case INITIALIZE:
                responseData = McpService.emitResult(uuid, mcpRequest.getId(), new McpInitializeData());
                break;
            case TOOLS_CALL:
                @SuppressWarnings("unchecked")
                Map<String, Object> toolParams = (Map<String, Object>) mcpRequest.getParams();
                String toolName = toolParams.get("name").toString();
                Method toolMethod = METHOD_MAP.get(toolName);
                if (Objects.isNull(toolMethod)) {
                    throw new McpException(McpErrorCode.MethodNotFound.getLabel());
                }
                McpToolDefinition mcpTool = getTool(toolMethod);
                if (Objects.isNull(mcpTool)) {
                    throw new McpException(McpErrorCode.MethodNotFound.getLabel());
                }
                Object callResult;
                try {
                    Class<?> declaringClass = toolMethod.getDeclaringClass();
                    Object bean = beanFactory.getBean(declaringClass);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> arguments = (Map<String, Object>) toolParams.get("arguments");

                    List<String> keys = new ArrayList<>(arguments.keySet());
                    Collections.sort(keys);

                    Map<String, Object> sortedArguments = keys.stream().collect(
                            Collectors.toMap(
                                    key -> key, arguments::get,
                                    (a, b) -> b, LinkedHashMap::new
                            )
                    );
                    Object[] args = sortedArguments.values().toArray();
                    callResult = toolMethod.invoke(bean, args);
                } catch (Exception e) {
                    if (e instanceof InvocationTargetException) {
                        callResult = ((InvocationTargetException) e).getTargetException().getMessage();
                    } else {
                        callResult = e.getMessage();
                    }
                }
                McpCallToolResponse mcpCallToolResponse = new McpCallToolResponse();
                if (Objects.isNull(callResult) || !StringUtils.hasText(callResult.toString())) {
                    callResult = "操作成功";
                }
                mcpCallToolResponse.getContent().add(
                        new McpContent().setText(callResult.toString())
                );
                responseData = emitResult(uuid, mcpRequest.getId(), mcpCallToolResponse);
                break;
            case TOOLS_LIST:
                responseData = McpService.emitResult(uuid, mcpRequest.getId(), Map.of(
                        "tools", McpService.tools
                ));
                break;
            case NOTIFICATIONS_INITIALIZED:
                responseData = null;
                break;
            case NOTIFICATION_CANCELLED:
                responseData = null;
                break;
            case PING:
                responseData = McpService.emitResult(uuid, mcpRequest.getId(), "pong");
                break;
            case PROMPTS_LIST:
                responseData = McpService.emitResult(uuid, mcpRequest.getId(), Map.of(
                        "prompts", McpService.prompts
                ));
                break;
            case PROMPTS_GET:
                @SuppressWarnings("unchecked")
                Map<String, Object> promptParams = (Map<String, Object>) mcpRequest.getParams();
                String promptName = promptParams.get("name").toString();
                Method promptMethod = PROMPT_MAP.get(promptName);
                if (Objects.isNull(promptMethod)) {
                    throw new McpException(McpErrorCode.MethodNotFound.getLabel());
                }
                McpPromptDefinition promptDefinition = getPrompt(promptMethod);
                if (Objects.isNull(promptDefinition)) {
                    throw new McpException(McpErrorCode.MethodNotFound.getLabel());
                }
                Object getResult;
                try {
                    Class<?> declaringClass = promptMethod.getDeclaringClass();
                    Object bean = beanFactory.getBean(declaringClass);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> arguments = (Map<String, Object>) promptParams.get("arguments");

                    List<String> keys = new ArrayList<>(arguments.keySet());
                    Collections.sort(keys);

                    Map<String, Object> sortedArguments = keys.stream().collect(
                            Collectors.toMap(
                                    key -> key, arguments::get,
                                    (a, b) -> b, LinkedHashMap::new
                            )
                    );
                    Object[] args = sortedArguments.values().toArray();
                    getResult = promptMethod.invoke(bean, args);
                } catch (Exception e) {
                    if (e instanceof InvocationTargetException) {
                        getResult = ((InvocationTargetException) e).getTargetException().getMessage();
                    } else {
                        getResult = e.getMessage();
                    }
                }
                if (Objects.isNull(getResult) || !StringUtils.hasText(getResult.toString())) {
                    getResult = "操作成功";
                }
                McpContent content = new McpContent().setText(getResult.toString());
                responseData = emitResult(uuid, mcpRequest.getId(), Map.of("description", promptDefinition.getDescription()
                        ,"messages", List.of(McpGetPromptResponse.withUserRole(content))));
                break;
            default:
                throw new McpException(McpErrorCode.MethodNotFound.getLabel());
        }
        return responseData;
    }

}
