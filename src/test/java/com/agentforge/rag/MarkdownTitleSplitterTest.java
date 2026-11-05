package com.agentforge.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 分块器契约（mvp-scope M6：标题/表格/无标题文档） */
class MarkdownTitleSplitterTest {

    private final MarkdownTitleSplitter splitter = new MarkdownTitleSplitter();

    @Test
    @DisplayName("按标题切分，各章节含标题原文与正文")
    void splitsByHeadings() {
        List<Splitter.Section> sections = splitter.split("""
                # 架构
                分层设计。

                ## 部署
                单机部署。
                """);
        assertThat(sections).hasSize(2);
        assertThat(sections.get(0).title()).isEqualTo("架构");
        assertThat(sections.get(0).content()).startsWith("# 架构").contains("分层设计");
        assertThat(sections.get(1).title()).isEqualTo("部署");
        assertThat(sections.get(1).content()).startsWith("## 部署");
    }

    @Test
    @DisplayName("首个标题前的前言段保留为无标题分块")
    void preambleBecomesUntitledSection() {
        List<Splitter.Section> sections = splitter.split("""
                项目使用说明。

                # 安装
                下载安装包。
                """);
        assertThat(sections).hasSize(2);
        assertThat(sections.get(0).title()).isNull();
        assertThat(sections.get(0).content()).isEqualTo("项目使用说明。");
        assertThat(sections.get(1).title()).isEqualTo("安装");
    }

    @Test
    @DisplayName("表格保留原始 Markdown（不做 AST 重组）")
    void tablePreservedAsRawMarkdown() {
        List<Splitter.Section> sections = splitter.split("""
                # 价格表
                | 商品 | 价格 |
                |---|---|
                | 手机 | 5999 |
                """);
        assertThat(sections.get(0).content())
                .contains("| 商品 | 价格 |")
                .contains("|---|---|")
                .contains("| 手机 | 5999 |");
    }

    @Test
    @DisplayName("无任何标题：整篇作为单个无标题分块")
    void noHeadingSingleSection() {
        List<Splitter.Section> sections = splitter.split("只有一段普通文本，没有标题。");
        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).title()).isNull();
        assertThat(sections.get(0).content()).contains("普通文本");
    }

    @Test
    @DisplayName("空文档返回空列表")
    void emptyDocument() {
        assertThat(splitter.split(null)).isEmpty();
        assertThat(splitter.split("   \n  ")).isEmpty();
    }
}
