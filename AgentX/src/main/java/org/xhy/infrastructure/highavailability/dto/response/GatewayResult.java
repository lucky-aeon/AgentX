package org.xhy.infrastructure.highavailability.dto.response;

/** 网关统一响应结果
 * 
 * @author xhy
 * @since 1.0.0 */
public class GatewayResult<T> {

    /** 响应码 */
    private Integer code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 是否成功 */
    private Boolean success;

    public GatewayResult() {
    }

    public GatewayResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = code != null && code == 200;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
        this.success = code != null && code == 200;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}