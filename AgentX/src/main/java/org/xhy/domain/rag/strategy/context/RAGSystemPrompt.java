package org.xhy.domain.rag.strategy.context;

public class RAGSystemPrompt {

    public static String OCR_PROMPT = """
            请识别图片中的内容，注意以下要求：

                                                对于数学公式和普通文本：

                                                所有数学公式和数学符号都必须使用标准的LaTeX格式

                                                行内公式使用单个$符号包裹，如：$x^2$

                                                独立公式块使用两个
                                                符号包裹，如：
                                                符号包裹，如：$$sum_{i=1}^n i^2$$

                                                普通文本保持原样，不要使用LaTeX格式

                                                保持原文的段落格式和换行

                                                明显的换行使用\\n表示

                                                确保所有数学符号都被正确包裹在$或$$中

                                                对于验证码图片：

                                                只输出验证码字符，不要加任何额外解释

                                                忽略干扰线和噪点

                                                注意区分相似字符，如0和O、1和l、2和Z等

                                                验证码通常为4-6位字母数字组合

            """;

}
