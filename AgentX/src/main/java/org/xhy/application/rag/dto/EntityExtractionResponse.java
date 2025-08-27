package org.xhy.application.rag.dto;

import java.util.List;

/**
 * LLM实体提取响应DTO
 * 
 * @author AgentX
 */
public class EntityExtractionResponse {
    
    private List<EntityItem> entities;
    
    public List<EntityItem> getEntities() {
        return entities;
    }
    
    public void setEntities(List<EntityItem> entities) {
        this.entities = entities;
    }
    
    /**
     * 实体项
     */
    public static class EntityItem {
        private String text;
        private String type;
        private double confidence;
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
        
        @Override
        public String toString() {
            return "EntityItem{" +
                    "text='" + text + '\'' +
                    ", type='" + type + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }
}
