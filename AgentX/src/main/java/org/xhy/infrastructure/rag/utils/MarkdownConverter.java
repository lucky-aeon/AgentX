package org.xhy.infrastructure.rag.utils;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * @author shilong.zang
 * @date 19:00 <br/>
 */
public class MarkdownConverter {

    private static final Parser PARSER;

    private static final HtmlRenderer RENDERER;

    static {
        final MutableDataSet options = new MutableDataSet();
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();
    }

    public static String convertMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        // 解析Markdown文本
        var document = PARSER.parse(markdown);
        // 渲染为HTML
        return RENDERER.render(document);
    }


}
