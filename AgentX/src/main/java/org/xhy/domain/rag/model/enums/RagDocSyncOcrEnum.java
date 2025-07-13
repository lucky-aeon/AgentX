package org.xhy.domain.rag.model.enums;

import java.util.Set;

/** @author shilong.zang
 * @date 09:45 <br/>
 */
public enum RagDocSyncOcrEnum {

    /** pdf策略 */
    PDF(Set.of("PDF"), "ragDocSyncOcr-PDF"),

    /** word策略 */
    DOCX(Set.of("DOC", "DOCX", "PPT", "PPTX", "XLS", "XLSX"), "ragDocSyncOcr-WORD"),

    /** 纯文本策略 */
    TXT(Set.of("TXT", "HTML", "MD"), "ragDocSyncOcr-TXT"),

    ;

    private final Set<String> value;
    private final String label;

    RagDocSyncOcrEnum(Set<String> value, String label) {
        this.value = value;
        this.label = label;
    }

    public static String getLabelByValue(String label) {
        for (RagDocSyncOcrEnum enumValue : RagDocSyncOcrEnum.values()) {
            if (enumValue.value.contains(label)) {
                return enumValue.label;
            }
        }
        return null;
    }

}
