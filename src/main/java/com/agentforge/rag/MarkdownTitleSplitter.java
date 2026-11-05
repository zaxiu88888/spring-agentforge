package com.agentforge.rag;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 按 Markdown 标题章节分块（对齐原型已验证的策略）：
 * flexmark 解析 AST 取所有顶层 Heading 的原文偏移量，按偏移量切片原文——
 * 表格/代码块等结构保留原始 Markdown，不做 AST 重组。
 */
@Component
public class MarkdownTitleSplitter implements Splitter {

    private final Parser parser = Parser.builder(new MutableDataSet()).build();

    @Override
    public List<Section> split(String rawMarkdown) {
        List<Section> sections = new ArrayList<>();
        if (rawMarkdown == null || rawMarkdown.isBlank()) {
            return sections;
        }

        record Boundary(String title, int start) {
        }
        List<Boundary> boundaries = new ArrayList<>();
        Node document = parser.parse(rawMarkdown);
        for (Node child = document.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Heading heading) {
                boundaries.add(new Boundary(
                        heading.getChars().toString().replaceFirst("^#+\\s*", "").trim(),
                        heading.getStartOffset()));
            }
        }

        // 第一个标题之前的前言段（无标题）
        int first = boundaries.isEmpty() ? rawMarkdown.length() : boundaries.get(0).start();
        addSection(sections, null, rawMarkdown.substring(0, first));

        // 各标题章节：从本标题起，到下一标题（或文末）
        for (int i = 0; i < boundaries.size(); i++) {
            int start = boundaries.get(i).start();
            int end = (i + 1 < boundaries.size()) ? boundaries.get(i + 1).start() : rawMarkdown.length();
            addSection(sections, boundaries.get(i).title(), rawMarkdown.substring(start, end));
        }
        return sections;
    }

    private void addSection(List<Section> sections, String title, String rawContent) {
        String content = rawContent.strip();
        if (content.isEmpty() && title == null) {
            return;   // 空前言直接丢弃；有标题的空章节保留（标题即信息）
        }
        sections.add(new Section(title, content));
    }
}
