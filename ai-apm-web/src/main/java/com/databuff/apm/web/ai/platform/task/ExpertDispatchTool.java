package com.databuff.apm.web.ai.platform.task;

import com.databuff.apm.web.ai.agent.AiSessionStore;
import com.databuff.apm.web.ai.platform.runtime.ExpertChatContext;
import com.databuff.apm.web.ai.platform.runtime.ExpertChatScopeRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ExpertDispatchTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectProvider<ExpertTaskService> expertTaskService;
    private final ObjectProvider<AiSessionStore> sessionStore;

    public ExpertDispatchTool(
            ObjectProvider<ExpertTaskService> expertTaskService,
            ObjectProvider<AiSessionStore> sessionStore) {
        this.expertTaskService = expertTaskService;
        this.sessionStore = sessionStore;
    }

    @Tool(description = "Dispatch a subtask to another digital expert asynchronously. "
            + "Same targetExpertId must be serial (wait for callback before dispatching again); "
            + "different targetExpertId values may run in parallel.")
    public String dispatchExpertTask(
            @ToolParam(name = "targetExpertId", description = "Target expert id from the brain's available digital experts catalog")
            String targetExpertId,
            @ToolParam(name = "task", description = "Task for the target expert: if one expert can finish the user request, pass the full user request; if multi-expert/step, pass organized info covering that step's goals without dropping user requirements or inventing extra metrics/fields")
            String task,
            @ToolParam(name = "contextJson", required = false, description = "Optional JSON context")
            String contextJson,
            RuntimeContext runtimeContext) {
        String sessionId = resolveDispatchSessionId(contextJson, runtimeContext);
        String normalizedTarget = targetExpertId == null ? "" : targetExpertId.trim();
        String sourceExpertId = resolveSourceExpertId(sessionId, normalizedTarget);
        ExpertTaskService service = expertTaskService.getObject();
        var inFlight = service.findInFlightForTarget(sessionId, normalizedTarget);
        if (inFlight.isPresent()) {
            ExpertTask busy = inFlight.get();
            return ExpertMessageConstants.serialDispatchBusyMessage(busy.taskId(), busy.targetExpertId());
        }
        try {
            ExpertTask created = service.submit(new ExpertTaskRequest(
                    sessionId,
                    sourceExpertId,
                    normalizedTarget,
                    task,
                    null,
                    enrichDispatchContext(contextJson, sessionId, sourceExpertId)));
            return ExpertMessageConstants.asyncWaitMessage(created.taskId(), created.targetExpertId());
        } catch (SerialExpertDispatchException e) {
            ExpertTask busy = e.inFlightTask();
            return ExpertMessageConstants.serialDispatchBusyMessage(
                    busy == null ? "?" : busy.taskId(),
                    busy == null ? normalizedTarget : busy.targetExpertId());
        }
    }

    /**
     * Record the real dispatcher (usually brain). Never attribute source as the target
     * (e.g. qa→qa) when a nested subtask chat-scope overwrote expertId.
     */
    private static String resolveSourceExpertId(String sessionId, String targetExpertId) {
        String source = ExpertTaskContext.outermostSourceExpertId(sessionId)
                .or(() -> ExpertTaskContext.sourceExpertId(sessionId))
                .or(() -> ExpertChatScopeRegistry.findParent(sessionId)
                        .map(ExpertChatContext.State::expertId))
                .orElse("brain");
        if (targetExpertId != null
                && !targetExpertId.isBlank()
                && targetExpertId.equals(source)) {
            return ExpertTaskContext.outermostSourceExpertId(sessionId)
                    .filter(id -> !targetExpertId.equals(id))
                    .orElse("brain");
        }
        return source;
    }

    private String resolveDispatchSessionId(String contextJson, RuntimeContext runtimeContext) {
        Map<String, Object> context = parseContext(contextJson);
        Object fromContext = context.get(ExpertMessageConstants.META_SESSION_ID);
        String hint = fromContext == null ? null : String.valueOf(fromContext);
        return ExpertSessionResolver.resolveSessionIdOrThrow(hint, runtimeContext);
    }

    private Map<String, Object> enrichDispatchContext(
            String contextJson,
            String sessionId,
            String sourceExpertId) {
        Map<String, Object> context = parseContext(contextJson);
        Map<String, Object> enriched = new LinkedHashMap<>(context);
        enriched.put(ExpertMessageConstants.META_SESSION_ID, sessionId);
        enriched.put(ExpertMessageConstants.META_SOURCE_EXPERT_ID, sourceExpertId);
        AiSessionStore store = sessionStore.getIfAvailable();
        if (store != null && sessionId != null && !sessionId.isBlank()) {
            enriched.putIfAbsent(ExpertMessageConstants.META_ROUND_INDEX, store.peekCurrentRoundIndex(sessionId));
            enriched.putIfAbsent("userName", store.peekUserName(sessionId));
        }
        return enriched;
    }

    private static Map<String, Object> parseContext(String contextJson) {
        if (contextJson == null || contextJson.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(contextJson, MAP_TYPE);
        } catch (Exception e) {
            return Map.of("rawContext", contextJson);
        }
    }
}
