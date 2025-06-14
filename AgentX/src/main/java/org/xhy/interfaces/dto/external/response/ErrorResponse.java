package org.xhy.interfaces.dto.external.response;

/** 统一错误响应对象 - 外部API使用 */
public class ErrorResponse {

    /** 错误信息 */
    private ErrorDetail error;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message, String details) {
        this.error = new ErrorDetail(code, message, details);
    }

    public ErrorDetail getError() {
        return error;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }

    /** 错误详情 */
    public static class ErrorDetail {
        /** 错误码 */
        private String code;

        /** 错误消息 */
        private String message;

        /** 错误详情 */
        private String details;

        public ErrorDetail() {
        }

        public ErrorDetail(String code, String message, String details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }
    }
}