package org.xhy.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/** 内容缓存过滤器 用于包装请求和响应，使得可以重复读取请求体和响应体内容 */
@Component
@Order(1) // 确保在其他过滤器之前执行
public class ContentCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 包装请求和响应，使其内容可以被重复读取
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            // 继续过滤器链
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // 将响应内容写回到原始响应中
            wrappedResponse.copyBodyToResponse();
        }
    }
}
