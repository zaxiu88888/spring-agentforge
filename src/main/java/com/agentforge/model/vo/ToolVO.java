package com.agentforge.model.vo;

/** GET /api/tools 返回项（api-contract.md §6：仅 OPTIONAL 工具供 Agent 表单勾选） */
public record ToolVO(String name, String description, String type) {
}
