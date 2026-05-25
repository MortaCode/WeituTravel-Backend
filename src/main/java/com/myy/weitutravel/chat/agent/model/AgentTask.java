package com.myy.weitutravel.chat.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 子任务定义 —— AgentPlan 中的一个步骤
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTask {

    /** 任务唯一标识 */
    private String id;
    /** 任务描述（给 LLM 看） */
    private String description;
    /** 需要调用的 MCP 工具名称 */
    private String toolName;
    /** 工具用途说明 */
    private String toolPurpose;
    /** 执行顺序（越小越先执行） */
    private int order;
    /** 依赖的前置任务 ID 列表 */
    @Builder.Default
    private List<String> dependencies = new ArrayList<>();
    /** 传递给工具的固定参数（从意图实体中提取） */
    @Builder.Default
    private Map<String, String> params = Map.of();
    /** 是否必须执行 */
    @Builder.Default
    private boolean required = true;
    /** 任务状态 */
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    public enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
    }
}
