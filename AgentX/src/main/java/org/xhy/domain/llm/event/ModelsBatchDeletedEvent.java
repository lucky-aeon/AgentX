package org.xhy.domain.llm.event;

import java.util.List;

/** 模型批量删除事件
 * 
 * @author xhy
 * @since 1.0.0 */
public class ModelsBatchDeletedEvent {

    /** 删除项列表 */
    private final List<ModelDeleteItem> deleteItems;

    /** 用户ID */
    private final String userId;

    public ModelsBatchDeletedEvent(List<ModelDeleteItem> deleteItems, String userId) {
        this.deleteItems = deleteItems;
        this.userId = userId;
    }

    public List<ModelDeleteItem> getDeleteItems() {
        return deleteItems;
    }

    public String getUserId() {
        return userId;
    }

    /** 模型删除项 */
    public static class ModelDeleteItem {
        private final String modelId;
        private final String userId;

        public ModelDeleteItem(String modelId, String userId) {
            this.modelId = modelId;
            this.userId = userId;
        }

        public String getModelId() {
            return modelId;
        }

        public String getUserId() {
            return userId;
        }
    }
}