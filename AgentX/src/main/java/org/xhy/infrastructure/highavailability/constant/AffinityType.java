package org.xhy.infrastructure.highavailability.constant;

/** 亲和性类型常量
 * 
 * @author xhy
 * @since 1.0.0 */
public class AffinityType {

    /** 会话亲和性 - 同一会话的请求路由到同一实例 */
    public static final String SESSION = "SESSION";

    /** 用户亲和性 - 同一用户的请求路由到同一实例 */
    public static final String USER = "USER";

    /** 批次亲和性 - 同一批次的请求路由到同一实例 */
    public static final String BATCH = "BATCH";

    /** 地域亲和性 - 同一地域的请求路由到同一实例 */
    public static final String REGION = "REGION";

    private AffinityType() {
        // 工具类，禁止实例化
    }
}