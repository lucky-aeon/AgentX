package org.xhy.infrastructure.logging.constant;

/** MDC常量定义类 用于统一日志格式化输出的字段定义 */
public class MdcConstants {

    public static final String HEADER_X_UPSTREAM_ADDR = "X-Upstream-Addr";

    /** 调用方式 */
    public static final String MDC_KIND = "kind";

    /** 请求来源ip */
    public static final String MDC_UPSTREAM_IP = "upstream";

    /** 请求地址 */
    public static final String MDC_SERVER_URL = "server_url";

    /** 请求方法 post get */
    public static final String MDC_SERVER_METHOD = "server_method";

    /** 请求状态码 */
    public static final String MDC_STATUS_CODE = "status_code";

    /** 耗时 ms */
    public static final String MDC_COST = "cost";

    /** 请求参数 */
    public static final String MDC_ARGS = "args";

    /** http协议的Header里面的host头 */
    public static final String MDC_HOST = "host";

    /** 发起请求的客户端ip地址 */
    public static final String MDC_REMOTE_ADDR = "remote_addr";

    /** Header 名 - 链路追踪编号 */
    public static final String MDC_TRACE_ID = "x-agentx-traceId";

    private MdcConstants() {
        // 工具类，禁止实例化
    }
}
