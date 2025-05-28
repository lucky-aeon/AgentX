package org.xhy.infrastructure.exception;

public class StreamInterruptedException extends RuntimeException {
    private String partialContent;

    public StreamInterruptedException(String partialContent) {
        super("流处理被中断。"); // 或者更具体的消息
        this.partialContent = partialContent;
    }

    public StreamInterruptedException(String message, String partialContent) {
        super(message);
        this.partialContent = partialContent;
    }

    public String getPartialContent() {
        return partialContent;
    }
}