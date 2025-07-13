package org.xhy.application.rag.dto;

import java.util.List;

/**
 * RAG流式响应DTO
 * 
 * @author shilong.zang
 */
public class RagStreamResponse {
    
    /**
     * 阶段类型：retrieval, thinking, answer
     */
    private String stage;
    
    /**
     * 状态：start, progress, end
     */
    private String status;
    
    /**
     * 消息内容
     */
    private String message;
    
    /**
     * 检索到的文档信息（仅在retrieval阶段的end状态返回）
     */
    private List<RetrievedDocument> retrievedDocuments;
    
    /**
     * 答案片段（仅在answer阶段的progress状态返回）
     */
    private String answerFragment;
    
    /**
     * 是否完成
     */
    private Boolean finished = false;
    
    /**
     * 错误信息
     */
    private String error;

    /**
     * 检索到的文档信息
     */
    public static class RetrievedDocument {
        private String fileId;
        private String fileName;
        private String documentId;
        private Double score;

        public RetrievedDocument() {}

        public RetrievedDocument(String fileId, String fileName, String documentId, Double score) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.documentId = documentId;
            this.score = score;
        }

        public String getFileId() {
            return fileId;
        }

        public void setFileId(String fileId) {
            this.fileId = fileId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }

    // 静态工厂方法
    public static RagStreamResponse retrievalStart(String message) {
        RagStreamResponse response = new RagStreamResponse();
        response.setStage("retrieval");
        response.setStatus("start");
        response.setMessage(message);
        return response;
    }

    public static RagStreamResponse retrievalProgress(String message) {
        RagStreamResponse response = new RagStreamResponse();
        response.setStage("retrieval");
        response.setStatus("progress");
        response.setMessage(message);
        return response;
    }

    public static RagStreamResponse retrievalEnd(String message, List<RetrievedDocument> documents) {
        RagStreamResponse response = new RagStreamResponse();
        response.setStage("retrieval");
        response.setStatus("end");
        response.setMessage(message);
        response.setRetrievedDocuments(documents);
        return response;
    }

    public static RagStreamResponse thinkingStart(String message) {
        RagStreamResponse response = new RagStreamResponse();
        response.setStage("thinking");
        response.setStatus("start");
        response.setMessage(message);
        return response;
    }

    public static RagStreamResponse thinkingProgress(String message) {
        RagStreamResponse response = new RagStreamResponse();
        response.setStage("thinking");
        response.setStatus("progress");
        response.setMessage(message);
        return response;
    }

    public static RagStreamResponse thinkingEnd(String message) {
        RagStreamResponse response = new RagStreamResponse();
        response.setStage("thinking");
        response.setStatus("end");
        response.setMessage(message);
        return response;
    }

    public static RagStreamResponse answerStart(String message) {
        RagStreamResponse response = new RagStreamResponse();
        response.setStage("answer");
        response.setStatus("start");
        response.setMessage(message);
        return response;
    }

    public static RagStreamResponse answerProgress(String answerFragment) {
        RagStreamResponse response = new RagStreamResponse();
        response.setStage("answer");
        response.setStatus("progress");
        response.setAnswerFragment(answerFragment);
        return response;
    }

    public static RagStreamResponse answerEnd(String message) {
        RagStreamResponse response = new RagStreamResponse();
        response.setStage("answer");
        response.setStatus("end");
        response.setMessage(message);
        response.setFinished(true);
        return response;
    }

    public static RagStreamResponse error(String errorMessage) {
        RagStreamResponse response = new RagStreamResponse();
        response.setError(errorMessage);
        response.setFinished(true);
        return response;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<RetrievedDocument> getRetrievedDocuments() {
        return retrievedDocuments;
    }

    public void setRetrievedDocuments(List<RetrievedDocument> retrievedDocuments) {
        this.retrievedDocuments = retrievedDocuments;
    }

    public String getAnswerFragment() {
        return answerFragment;
    }

    public void setAnswerFragment(String answerFragment) {
        this.answerFragment = answerFragment;
    }

    public Boolean getFinished() {
        return finished;
    }

    public void setFinished(Boolean finished) {
        this.finished = finished;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}