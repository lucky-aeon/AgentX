package org.xhy.infrastructure.logging.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.xhy.infrastructure.logging.constant.MdcConstants;

/**
 * 统一日志拦截器
 * 收集请求信息并设置MDC字段，用于统一JSON格式日志输出
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String START_TIME_ATTR = "startTime";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler) {
        
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTR, startTime);

        // 设置链路追踪ID
        String traceId = getOrGenerateTraceId(request);
        MDC.put(MdcConstants.MDC_TRACE_ID, traceId);
        
        // 设置调用方式
        MDC.put(MdcConstants.MDC_KIND, "http");
        
        // 设置请求来源IP (优先从X-Upstream-Addr获取，其次从X-Forwarded-For，最后从remote address)
        String upstreamIp = getUpstreamIp(request);
        MDC.put(MdcConstants.MDC_UPSTREAM_IP, upstreamIp);
        
        // 设置请求地址
        String serverUrl = getFullRequestUrl(request);
        MDC.put(MdcConstants.MDC_SERVER_URL, serverUrl);
        
        // 设置请求方法
        MDC.put(MdcConstants.MDC_SERVER_METHOD, request.getMethod().toUpperCase());
        
        // 设置Host头
        String host = request.getHeader("Host");
        MDC.put(MdcConstants.MDC_HOST, StringUtils.hasText(host) ? host : "unknown");
        
        // 设置客户端真实IP
        String remoteAddr = getClientIpAddress(request);
        MDC.put(MdcConstants.MDC_REMOTE_ADDR, remoteAddr);
        
        // 设置请求参数
        String args = getRequestArgs(request);
        MDC.put(MdcConstants.MDC_ARGS, args);

        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, 
                              @NonNull HttpServletResponse response, 
                              @NonNull Object handler, 
                              Exception ex) {
        
        try {
            // 计算耗时
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            if (startTime != null) {
                long cost = System.currentTimeMillis() - startTime;
                MDC.put(MdcConstants.MDC_COST, String.valueOf(cost));
            }
            
            // 设置状态码
            MDC.put(MdcConstants.MDC_STATUS_CODE, String.valueOf(response.getStatus()));
            
            // 记录访问日志
            logger.info("HTTP Request Completed");
            
        } finally {
            // 清理MDC，避免内存泄漏
            clearMdc();
        }
    }

    /**
     * 获取或生成链路追踪ID
     */
    private String getOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(MdcConstants.MDC_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            // 如果请求头中没有traceId，则生成一个新的
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }

    /**
     * 获取上游IP地址
     */
    private String getUpstreamIp(HttpServletRequest request) {
        String upstreamAddr = request.getHeader(MdcConstants.HEADER_X_UPSTREAM_ADDR);
        if (StringUtils.hasText(upstreamAddr)) {
            return upstreamAddr;
        }
        
        // 尝试从X-Forwarded-For获取
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // X-Forwarded-For可能包含多个IP，取第一个
            return xForwardedFor.split(",")[0].trim();
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 多个IP的情况，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 获取完整的请求URL
     */
    private String getFullRequestUrl(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();
        
        if (StringUtils.hasText(queryString)) {
            requestURL.append("?").append(queryString);
        }
        
        return requestURL.toString();
    }

    /**
     * 获取请求参数
     */
    private String getRequestArgs(HttpServletRequest request) {
        try {
            // 对于GET请求，获取查询参数
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                String queryString = request.getQueryString();
                return StringUtils.hasText(queryString) ? queryString : "";
            }
            
            // 对于POST/PUT等请求，尝试获取请求体
            if (request instanceof ContentCachingRequestWrapper) {
                ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
                byte[] content = wrapper.getContentAsByteArray();
                if (content.length > 0) {
                    String contentType = request.getContentType();
                    if (contentType != null && contentType.contains("application/json")) {
                        String jsonString = new String(content, StandardCharsets.UTF_8);
                        // 压缩JSON为单行格式，去除换行和多余空格
                        return compactJson(jsonString);
                    }
                }
            }
            
            return "";
        } catch (Exception e) {
            logger.warn("Failed to get request args: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 压缩JSON字符串，去除换行和多余空格
     */
    private String compactJson(String jsonString) {
        try {
            // 尝试解析并重新序列化为紧凑格式
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            return objectMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            // 如果解析失败，手动去除换行和多余空格
            return jsonString.replaceAll("\\s+", " ").trim();
        }
    }

    /**
     * 清理MDC上下文
     */
    private void clearMdc() {
        MDC.remove(MdcConstants.MDC_KIND);
        MDC.remove(MdcConstants.MDC_UPSTREAM_IP);
        MDC.remove(MdcConstants.MDC_SERVER_URL);
        MDC.remove(MdcConstants.MDC_SERVER_METHOD);
        MDC.remove(MdcConstants.MDC_STATUS_CODE);
        MDC.remove(MdcConstants.MDC_COST);
        MDC.remove(MdcConstants.MDC_ARGS);
        MDC.remove(MdcConstants.MDC_HOST);
        MDC.remove(MdcConstants.MDC_REMOTE_ADDR);
        MDC.remove(MdcConstants.MDC_TRACE_ID);
    }
}
