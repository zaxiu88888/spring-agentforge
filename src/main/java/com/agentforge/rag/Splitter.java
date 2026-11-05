package com.agentforge.rag;

import java.util.List;

/**
 * 文档分块策略接口（tech-design.md 拓展缝）：MVP 实现按 Markdown 标题分块；
 * 未来可加固定 token 滑窗 / 递归切分等实现，按 KB 或 Agent 配置选择。
 */
public interface Splitter {

    List<Section> split(String rawMarkdown);

    /** 一个分块：标题（无标题前言段为 null）+ 原文内容 */
    record Section(String title, String content) {
    }
}
