package com.databuff.apm.web.ai.platform.task;

public final class ExpertMessageConstants {

    public static final String META_SESSION_ID = "sessionId";
    public static final String META_ROUND_INDEX = "roundIndex";
    public static final String META_TASK_ID = "taskId";
    public static final String META_SOURCE_EXPERT_ID = "sourceExpertId";
    public static final String META_TRIGGER_SOURCE = "triggerSource";
    public static final String META_RUNTIME_SESSION_ID = "runtimeSessionId";
    public static final String META_IS_EXPERT_DELIVERABLE = "isExpertDeliverable";
    public static final String META_IS_ROUND_FINAL = "isRoundFinal";

    public static final String TRIGGER_USER = "user";
    public static final String TRIGGER_EXPERT_DISPATCH = "expert_dispatch";
    public static final String TRIGGER_EXPERT_RESULT = "expert_result";
    public static final String TRIGGER_BRAIN_CONTINUE = "brain_continue";

    public static final String CONTEXT_SESSION_PREFIX = "[Context: sessionId=";
    public static final String CONTEXT_ROUND_PREFIX = "[Context: roundIndex=";
    public static final String CONTEXT_TASK_PREFIX = "[Context: taskId=";
    public static final String CONTEXT_SOURCE_EXPERT_PREFIX = "[Context: sourceExpertId=";

    private ExpertMessageConstants() {
    }

    public static String asyncWaitMessage(String taskId, String targetExpertId) {
        return "异步任务已受理，taskId=" + taskId
                + ", targetExpertId=" + targetExpertId
                + "。请静静等待，完成后将通过内部通道回传结果。";
    }

    /** Returned when a second dispatch to the same target is rejected while one is still in flight. */
    public static String serialDispatchBusyMessage(String taskId, String targetExpertId) {
        return "该专家已有进行中的任务，禁止并行重复派发。"
                + " taskId=" + taskId
                + ", targetExpertId=" + targetExpertId
                + "。请等待系统注入该任务的回调结果后，再决定是否串行再次派发。";
    }

    /**
     * @param allAsyncComplete after pending-1 for this response, whether session pending reached 0
     */
    public static String expertResultContinueHint(boolean failure, boolean allAsyncComplete) {
        return expertResultContinueHint(failure, allAsyncComplete, null);
    }

    /**
     * @param originalUserRequest optional verbatim user message for this round; when present it is
     *                            echoed so brain must check remaining goals against it
     */
    public static String expertResultContinueHint(
            boolean failure,
            boolean allAsyncComplete,
            String originalUserRequest) {
        String userBlock = "";
        if (originalUserRequest != null && !originalUserRequest.isBlank()) {
            userBlock = "\n[本轮用户原请求]\n" + originalUserRequest.trim() + "\n";
        }
        if (!allAsyncComplete) {
            if (failure) {
                return "\n---\n[系统] 异步子任务失败。本 session 仍有未完成的异步 task（pending>0）。"
                        + userBlock
                        + "请消化本条结果并说明影响；勿输出最终 TEXT。"
                        + "中间回答属于内部过程，前端可能折叠。\n";
            }
            return "\n---\n[系统] 以上为异步子任务返回。本 session 仍有未完成的异步 task（pending>0）。"
                    + userBlock
                    + "请消化本条结果；勿输出最终 TEXT。"
                    + "中间回答属于内部过程，前端可能折叠。\n";
        }
        if (failure) {
            return "\n---\n[系统] 本 session 当前已派出的异步子任务均已结束（pending=0）。"
                    + userBlock
                    + "以上为最后一条子任务失败回传。"
                    + "先逐条对照「本轮用户原请求」列出尚未完成的子目标；若仍可改派其他专家推进，继续 dispatchExpertTask；"
                    + "仅当无法继续推进时，再输出自包含最终 TEXT："
                    + "汇总本轮已收到的专家结论，说明失败影响与可行下一步。"
                    + "禁止在未完成全部子目标时声称「用户原请求已全部覆盖」。\n";
        }
        return "\n---\n[系统] 本 session 当前已派出的异步子任务均已结束（pending=0）。"
                + userBlock
                + "以上为最后一条子任务返回。"
                + "pending=0 不等于用户原请求已完成。"
                + "你必须先逐条对照上方「本轮用户原请求」，列出尚未完成的子目标"
                + "（例如：已查出目标服务，但尚未巡检 / 尚未生成巡检报告）。"
                + "若列表非空：禁止输出最终 TEXT，必须继续 dispatchExpertTask，"
                + "并为目标专家整理完整可执行信息（含用户原约束与前序结论，交付物如「生成巡检报告」不得省略）。"
                + "仅当列表为空（用户原请求全部子目标均已完成）后，再输出自包含最终 TEXT，"
                + "重新呈现本轮所有专家的详细结论、关键数据、证据和建议；"
                + "不得仅给摘要，也不得使用“如上”“上一轮已说明”等引用中间过程的表述。"
                + "禁止在未完成全部子目标时声称「用户原请求已全部覆盖」。\n";
    }
}
