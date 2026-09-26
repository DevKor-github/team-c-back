package devkor.com.teamcback.domain.chatbot.gateway;

import static devkor.com.teamcback.global.response.ResultCode.CHATBOT_TEMPORARILY_UNAVAILABLE;

import devkor.com.teamcback.domain.chatbot.config.ChatbotProperties;
import devkor.com.teamcback.domain.chatbot.dto.ResolvedLocation;
import devkor.com.teamcback.domain.chatbot.service.ResolvedLocationCollector;
import devkor.com.teamcback.domain.chatbot.service.ChatbotToolCallLimiter;
import devkor.com.teamcback.domain.chatbot.service.ToolCallLimitExceededException;
import devkor.com.teamcback.domain.chatbot.tool.CampusChatbotTools;
import devkor.com.teamcback.global.exception.exception.GlobalException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "chatbot", name = "enabled", havingValue = "true")
public class SpringAiLlmGateway implements LlmGateway {
    private static final String TOOL_LIMIT_FALLBACK = "요청을 처리하는 데 필요한 확인이 너무 많습니다. "
            + "장소나 조건을 조금 더 구체적으로 알려 주세요.";
    private final ChatClient chatClient;
    private final ChatbotProperties properties;
    private final ExecutorService chatbotLlmExecutor;
    private final CampusChatbotTools campusChatbotTools;
    private final ChatbotToolCallLimiter toolCallLimiter;
    /** Enables stack-frame diagnostics without ever logging prompt/tool payloads. */
    @Value("${chatbot.llm.diagnostics-enabled:false}")
    private boolean diagnosticsEnabled;

    public SpringAiLlmGateway(ChatClient.Builder chatClientBuilder, ChatbotProperties properties,
                              ExecutorService chatbotLlmExecutor, CampusChatbotTools campusChatbotTools,
                              ChatbotToolCallLimiter toolCallLimiter) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
        this.chatbotLlmExecutor = chatbotLlmExecutor;
        this.campusChatbotTools = campusChatbotTools;
        this.toolCallLimiter = toolCallLimiter;
    }

    @Override
    public LlmResult generate(String systemPrompt, List<ConversationMessage> history, String userMessage) {
        ResolvedLocationCollector collector = new ResolvedLocationCollector();
        Future<GatewayResult> response = chatbotLlmExecutor.submit(
                () -> invoke(systemPrompt, history, userMessage, collector));
        try {
            GatewayResult result = response.get(properties.llm().timeout().toMillis(), TimeUnit.MILLISECONDS);
            log.info("chatbot_llm outcome={} provider={} model={} latencyMs={} toolCalls={} inputTokens=unavailable outputTokens=unavailable",
                    result.outcome(), properties.llm().provider(), properties.llm().model(), result.latencyMillis(),
                    result.toolCalls());
            return result.result();
        } catch (InterruptedException exception) {
            response.cancel(true);
            Thread.currentThread().interrupt();
            logFailure("TEMPORARILY_UNAVAILABLE", "ASYNC_WAIT", exception, false);
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        } catch (TimeoutException exception) {
            response.cancel(true);
            if (collector.hasNavigateIntent()) {
                logFailure("NAVIGATE_ROUTE_FAIL_SAFE", "ASYNC_WAIT_AFTER_TOOL_EXECUTION", exception, true);
                return new LlmResult(null, collector.snapshot(),
                        LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION,
                        collector.searchResolutionSnapshot());
            }
            logFailure("TEMPORARILY_UNAVAILABLE", "ASYNC_WAIT", exception, true);
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        } catch (ExecutionException exception) {
            response.cancel(true);
            if (hasCause(exception, ToolCallLimitExceededException.class)) {
                log.info("chatbot_llm outcome=TOOL_LIMIT provider={} model={}",
                        properties.llm().provider(), properties.llm().model());
                return new LlmResult(TOOL_LIMIT_FALLBACK, List.of());
            }
            logFailure("TEMPORARILY_UNAVAILABLE", failureStage(exception), exception, false);
            throw new GlobalException(CHATBOT_TEMPORARILY_UNAVAILABLE);
        }
    }

    private GatewayResult invoke(String systemPrompt, List<ConversationMessage> history, String userMessage,
                                 ResolvedLocationCollector collector) {
        long startedAt = System.nanoTime();
        try (ChatbotToolCallLimiter.Scope scope = toolCallLimiter.open()) {
            try {
                CampusChatbotTools requestTools = campusChatbotTools.forRequest(collector, scope);
                ChatClient.CallResponseSpec callResponse = chatClient.prompt()
                        .system(systemPrompt)
                        .messages(toSpringMessages(history, userMessage))
                        .tools(requestTools)
                        .call();
                ChatClientResponse clientResponse = callResponse == null ? null : callResponse.chatClientResponse();
                ChatResponse chatResponse = clientResponse == null ? null : clientResponse.chatResponse();
                String content = extractContent(chatResponse);
                if (content == null || content.isBlank()) {
                    logEmptyCompletionDiagnostics(chatResponse, scope, collector);
                    throw new EmptyLlmCompletionException();
                }
                return new GatewayResult(new LlmGateway.LlmResult(content, collector.snapshot(),
                        LlmGateway.CompletionStatus.COMPLETE, collector.searchResolutionSnapshot()), "SUCCESS",
                        elapsedMillis(startedAt), scope.callCount());
            } catch (RuntimeException exception) {
                if (hasCause(exception, ToolCallLimitExceededException.class)) {
                    return new GatewayResult(new LlmGateway.LlmResult(TOOL_LIMIT_FALLBACK, java.util.List.of()),
                            "TOOL_LIMIT", elapsedMillis(startedAt),
                            scope.callCount());
                }
                List<ResolvedLocation> resolvedLocations = collector.snapshot();
                String stage = resolvedLocations.isEmpty() ? "MODEL_TOOL_LOOP" : "POST_TOOL_EXECUTION";
                if (collector.hasNavigateIntent()) {
                    logFailure("NAVIGATE_ROUTE_FAIL_SAFE", stage, exception, false);
                    return new GatewayResult(new LlmGateway.LlmResult(null, resolvedLocations,
                            LlmGateway.CompletionStatus.FAILED_AFTER_TOOL_EXECUTION,
                            collector.searchResolutionSnapshot()),
                            "NAVIGATE_ROUTE_FAIL_SAFE", elapsedMillis(startedAt), scope.callCount());
                }
                throw new StagedLlmInvocationException(stage, exception);
            }
        }
    }

    private void logEmptyCompletionDiagnostics(ChatResponse response, ChatbotToolCallLimiter.Scope scope,
                                               ResolvedLocationCollector collector) {
        if (!diagnosticsEnabled) {
            return;
        }
        if (response == null) {
            log.error("chatbot_llm empty_completion responseNull=true generationCount=0 "
                            + "toolCallCount=0 toolCallNames=[] toolCallsPresent=false "
                            + "textState=unavailable finishReasons=[] metadataClass=unavailable "
                            + "toolCalls={} resolvedLocationCount={} searchResolutionCount={} "
                            + "navigateIntent={} ambiguousTrace={}",
                    scope.callCount(), collector.snapshot().size(), collector.searchResolutionSnapshot().size(),
                    collector.hasNavigateIntent(), hasAmbiguousTrace(collector));
            return;
        }

        List<Generation> generations = response.getResults() == null ? List.of() : response.getResults();
        List<String> toolNames = new ArrayList<>();
        int outputCount = 0;
        int assistantCount = 0;
        int blankTextCount = 0;
        List<String> finishReasons = new ArrayList<>();
        for (Generation generation : generations) {
            if (generation == null) {
                continue;
            }
            if (generation.getOutput() != null) {
                outputCount++;
                AssistantMessage assistant = generation.getOutput();
                assistantCount++;
                String text = assistant.getText();
                if (text == null || text.isBlank()) {
                    blankTextCount++;
                }
                if (assistant.getToolCalls() != null) {
                    assistant.getToolCalls().forEach(toolCall -> {
                        if (toolCall != null && toolCall.name() != null) {
                            toolNames.add(toolCall.name());
                        }
                    });
                }
            }
            ChatGenerationMetadata metadata = generation.getMetadata();
            if (metadata != null && metadata.getFinishReason() != null) {
                finishReasons.add(metadata.getFinishReason());
            }
        }
        log.error("chatbot_llm empty_completion responseNull=false generationCount={} outputCount={} "
                        + "assistantCount={} blankTextCount={} toolCallCount={} toolCallNames={} "
                        + "toolCallsPresent={} finishReasons={} responseMetadataClass={} "
                        + "toolCalls={} resolvedLocationCount={} searchResolutionCount={} "
                        + "navigateIntent={} ambiguousTrace={}",
                generations.size(), outputCount, assistantCount, blankTextCount, toolNames.size(), toolNames,
                !toolNames.isEmpty(), finishReasons,
                response.getMetadata() == null ? "unavailable" : response.getMetadata().getClass().getName(),
                scope.callCount(), collector.snapshot().size(), collector.searchResolutionSnapshot().size(),
                collector.hasNavigateIntent(), hasAmbiguousTrace(collector));
    }

    /** Mirrors ChatClient.content() extraction without issuing a second ChatModel call. */
    private String extractContent(ChatResponse response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return null;
        }
        Generation generation = response.getResults().get(0);
        if (generation == null || generation.getOutput() == null) {
            return null;
        }
        return generation.getOutput().getText();
    }

    private boolean hasAmbiguousTrace(ResolvedLocationCollector collector) {
        return collector.searchResolutionSnapshot().stream().anyMatch(trace -> trace.ambiguous());
    }

    private void logFailure(String outcome, String stage, Throwable exception, boolean timeout) {
        Throwable failure = unwrapExecutionException(exception);
        Throwable rootCause = rootCause(failure);
        String providerStatus = providerStatus(exception);
        boolean timedOut = timeout || isTimeout(exception, providerStatus);
        log.warn("chatbot_llm outcome={} stage={} provider={} model={} exceptionClass={} rootCauseClass={} "
                        + "safeMessage={} timeout={} providerStatus={}",
                outcome, stage, properties.llm().provider(), properties.llm().model(),
                failure.getClass().getName(), rootCause.getClass().getName(),
                safeMessage(exception, timedOut, providerStatus), timedOut,
                providerStatus == null ? "unavailable" : providerStatus);
        logDiagnosticFailure(stage, failure, rootCause, providerStatus, timedOut);
    }

    private void logDiagnosticFailure(String stage, Throwable failure, Throwable rootCause,
                                      String providerStatus, boolean timedOut) {
        if (!diagnosticsEnabled) {
            return;
        }
        log.error("chatbot_llm diagnostic stage={} exceptionClass={} rootCauseClass={} "
                        + "originalSafeMessage={} timeout={} providerStatus={} causeChain={} "
                        + "suppressedCount={} firstApplicationFrame={} firstSpringAiFrame={} "
                        + "firstProviderFrame={} stackFrames={}",
                stage, failure.getClass().getName(), rootCause.getClass().getName(),
                diagnosticMessage(rootCause), timedOut,
                providerStatus == null ? "unavailable" : providerStatus,
                causeChain(failure), suppressedCount(failure),
                firstFrame(failure, "devkor."), firstFrame(failure, "org.springframework.ai."),
                firstProviderFrame(failure), stackFrames(failure));
    }

    private String diagnosticMessage(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "<none>";
        }
        // Messages may contain model/tool arguments. Keep only a bounded, non-JSON hint.
        if (message.contains("{") || message.contains("}") || message.contains("\"")
                || message.length() > 240) {
            return "<redacted>";
        }
        return message.replaceAll("(?i)(token|secret|password|authorization|credential|coordinate|prompt)\\s*[:=].*",
                "$1=<redacted>");
    }

    private String causeChain(Throwable exception) {
        List<String> classes = new ArrayList<>();
        Throwable current = exception;
        while (current != null && classes.size() < 12) {
            classes.add(current.getClass().getName());
            current = current.getCause();
        }
        return String.join(" -> ", classes);
    }

    private int suppressedCount(Throwable exception) {
        int count = 0;
        Throwable current = exception;
        while (current != null) {
            count += current.getSuppressed().length;
            current = current.getCause();
        }
        return count;
    }

    private String firstFrame(Throwable exception, String packagePrefix) {
        Throwable current = exception;
        while (current != null) {
            for (StackTraceElement frame : current.getStackTrace()) {
                if (frame.getClassName().startsWith(packagePrefix)) {
                    return frame.toString();
                }
            }
            current = current.getCause();
        }
        return "unavailable";
    }

    private String firstProviderFrame(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            for (StackTraceElement frame : current.getStackTrace()) {
                String className = frame.getClassName();
                if (className.startsWith("com.google.") || className.startsWith("com.fasterxml.")
                        || className.startsWith("io.grpc.")) {
                    return frame.toString();
                }
            }
            current = current.getCause();
        }
        return "unavailable";
    }

    private String stackFrames(Throwable exception) {
        List<String> frames = new ArrayList<>();
        Throwable current = exception;
        while (current != null && frames.size() < 80) {
            for (StackTraceElement frame : current.getStackTrace()) {
                if (frames.size() >= 80) {
                    break;
                }
                frames.add(frame.toString());
            }
            current = current.getCause();
        }
        return String.join(" | ", frames);
    }

    private boolean isTimeout(Throwable exception, String providerStatus) {
        if ("DEADLINE_EXCEEDED".equals(providerStatus) || "REQUEST_TIMEOUT".equals(providerStatus)
                || "408".equals(providerStatus) || "504".equals(providerStatus)) {
            return true;
        }
        Throwable current = exception;
        while (current != null) {
            String className = current.getClass().getSimpleName();
            if (current instanceof TimeoutException || className.contains("Timeout")
                    || className.contains("DeadlineExceeded")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Throwable unwrapExecutionException(Throwable exception) {
        Throwable current = exception;
        while ((current instanceof ExecutionException || current instanceof StagedLlmInvocationException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private Throwable rootCause(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String failureStage(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof StagedLlmInvocationException staged) {
                return staged.stage();
            }
            current = current.getCause();
        }
        return "ASYNC_EXECUTION";
    }

    private String safeMessage(Throwable exception, boolean timeout, String providerStatus) {
        if (timeout) {
            return "LLM invocation timed out";
        }
        if (hasCause(exception, EmptyLlmCompletionException.class)) {
            return "Provider returned no final text completion";
        }
        if (providerStatus != null) {
            return "Provider request failed with status " + providerStatus;
        }
        return "LLM invocation failed";
    }

    private String providerStatus(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            try {
                Object status = current.getClass().getMethod("getStatusCode").invoke(current);
                if (status != null) {
                    Object code = invokeNoArg(status, "getCode");
                    if (code == null) {
                        code = invokeNoArg(status, "value");
                    }
                    String value = String.valueOf(code == null ? status : code);
                    if (value.matches("[A-Za-z0-9_.-]{1,64}")) {
                        return value;
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // Provider-neutral best effort: not every exception exposes a status.
            }
            current = current.getCause();
        }
        return null;
    }

    private Object invokeNoArg(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private List<Message> toSpringMessages(List<ConversationMessage> history, String userMessage) {
        List<Message> messages = new ArrayList<>();
        for (ConversationMessage message : history) {
            messages.add(switch (message.role()) {
                case USER -> new UserMessage(message.content());
                case ASSISTANT -> new AssistantMessage(message.content());
            });
        }
        messages.add(new UserMessage(userMessage));
        return List.copyOf(messages);
    }

    private boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
        Throwable current = exception;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private record GatewayResult(LlmGateway.LlmResult result, String outcome, long latencyMillis, int toolCalls) {
    }

    private static final class EmptyLlmCompletionException extends RuntimeException {
    }

    private static final class StagedLlmInvocationException extends RuntimeException {
        private final String stage;

        private StagedLlmInvocationException(String stage, RuntimeException cause) {
            super(cause);
            this.stage = stage;
        }

        private String stage() {
            return stage;
        }
    }
}
