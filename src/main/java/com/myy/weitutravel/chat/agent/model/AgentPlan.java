package com.myy.weitutravel.chat.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 任务计划 —— 任务拆分的输出结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPlan {

    /** 计划的旅行意图 */
    private TravelIntent intent;
    /** 子任务列表（已按依赖拓扑排序） */
    @Builder.Default
    private List<AgentTask> tasks = new ArrayList<>();
    /** 计划推理过程 */
    private String reasoning;
    /** 计划创建时间 */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 生成给 LLM 执行的提示词指令
     */
    public String toPromptInstructions() {
        if (tasks.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("## 当前任务执行计划\n");
        sb.append(reasoning != null ? reasoning : "请按以下步骤依次执行：").append("\n\n");

        for (int i = 0; i < tasks.size(); i++) {
            AgentTask task = tasks.get(i);
            sb.append("**步骤 ").append(i + 1).append("**：").append(task.getDescription()).append("\n");
            sb.append("  - 工具：").append(task.getToolName()).append("\n");
            sb.append("  - 目的：").append(task.getToolPurpose()).append("\n");
            if (!task.getParams().isEmpty()) {
                sb.append("  - 参数提示：").append(task.getParams()).append("\n");
            }
            if (!task.getDependencies().isEmpty()) {
                sb.append("  - 依赖：需等待步骤 ")
                        .append(String.join("、", task.getDependencies()))
                        .append(" 完成\n");
            }
            sb.append("\n");
        }

        sb.append("请严格按照上述步骤依次调用工具，每步完成后检查结果再进行下一步。");
        return sb.toString();
    }
}
