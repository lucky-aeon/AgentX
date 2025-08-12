package org.xhy.infrastructure.mq.enums;

/** @author zang
 * @date 13:39 <br/>
 */
public enum EventType {

    DOC_SYNC_RAG(4000, "文件入库"), DOC_REFRESH_ORG(4001, "文件ocr"),
    DOC_IE_INFER(4002,"实体识别关系抽取");

    private final Integer code;
    private final String desc;

    EventType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 按key获取枚举 */
    public static EventType getEnum(Integer code) {
        for (EventType e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
