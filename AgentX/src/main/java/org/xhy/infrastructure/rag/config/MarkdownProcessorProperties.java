package org.xhy.infrastructure.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Markdown处理器配置属性类 用于配置Markdown文档处理和段落拆分的相关参数 */
@Configuration
@ConfigurationProperties(prefix = "rag.markdown")
public class MarkdownProcessorProperties {

    /** 段落拆分配置 */
    private SegmentSplit segmentSplit = new SegmentSplit();

    /** 段落拆分配置内部类 */
    public static class SegmentSplit {
        /** 最大段落字符数 */
        private int maxLength = 1800;

        /** 最小段落字符数 */
        private int minLength = 200;

        /** 安全缓冲区大小 */
        private int bufferSize = 100;

        /** 是否启用段落拆分 */
        private boolean enabled = true;

        /** 是否启用重叠分段 */
        private boolean enableOverlap = false;

        /** 重叠区域大小 */
        private int overlapSize = 50;

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }

        public int getBufferSize() {
            return bufferSize;
        }

        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnableOverlap() {
            return enableOverlap;
        }

        public void setEnableOverlap(boolean enableOverlap) {
            this.enableOverlap = enableOverlap;
        }

        public int getOverlapSize() {
            return overlapSize;
        }

        public void setOverlapSize(int overlapSize) {
            this.overlapSize = overlapSize;
        }
    }

    public SegmentSplit getSegmentSplit() {
        return segmentSplit;
    }

    public void setSegmentSplit(SegmentSplit segmentSplit) {
        this.segmentSplit = segmentSplit;
    }
}