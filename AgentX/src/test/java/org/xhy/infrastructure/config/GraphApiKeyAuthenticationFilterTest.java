package org.xhy.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GraphApiKeyAuthenticationFilter单元测试 测试API密钥认证过滤器功能
 * 
 * @author zang */
@ExtendWith(MockitoExtension.class)
class GraphApiKeyAuthenticationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    /** 测试非图谱API请求直接通过 */
    @Test
    void testNonGraphApiRequestPassThrough() throws Exception {
        // Given
        GraphApiKeyAuthenticationFilter filter = new GraphApiKeyAuthenticationFilter();
        when(request.getRequestURI()).thenReturn("/api/v1/other");
        when(request.getMethod()).thenReturn("GET");

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    /** 测试健康检查端点无需认证 */
    @Test
    void testHealthEndpointNoAuth() throws Exception {
        // Given
        GraphApiKeyAuthenticationFilter filter = new GraphApiKeyAuthenticationFilter();
        when(request.getRequestURI()).thenReturn("/api/v1/graph/health");
        when(request.getMethod()).thenReturn("GET");

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    /** 测试认证被禁用时直接通过 */
    @Test
    void testAuthDisabledPassThrough() throws Exception {
        // Given
        GraphApiKeyAuthenticationFilter filter = new GraphApiKeyAuthenticationFilter();
        filter.setEnabled(false);
        when(request.getRequestURI()).thenReturn("/api/v1/graph/ingest");
        when(request.getMethod()).thenReturn("POST");

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    /** 测试API密钥缺失返回401 */
    @Test
    void testMissingApiKey() throws Exception {
        // Given
        GraphApiKeyAuthenticationFilter filter = new GraphApiKeyAuthenticationFilter();
        filter.setEnabled(true);
        filter.setHeaderName("X-Graph-API-Key");

        when(request.getRequestURI()).thenReturn("/api/v1/graph/ingest");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Graph-API-Key")).thenReturn(null);

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(response).setStatus(401);
        verify(response).setContentType(anyString());
        verify(chain, never()).doFilter(request, response);
    }

    /** 测试API密钥无效返回401 */
    @Test
    void testInvalidApiKey() throws Exception {
        // Given
        GraphApiKeyAuthenticationFilter filter = new GraphApiKeyAuthenticationFilter();
        filter.setEnabled(true);
        filter.setApiKey("correct-key");
        filter.setHeaderName("X-Graph-API-Key");

        when(request.getRequestURI()).thenReturn("/api/v1/graph/ingest");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Graph-API-Key")).thenReturn("wrong-key");

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(response).setStatus(401);
        verify(response).setContentType(anyString());
        verify(chain, never()).doFilter(request, response);
    }

    /** 测试API密钥正确时通过认证 */
    @Test
    void testValidApiKey() throws Exception {
        // Given
        GraphApiKeyAuthenticationFilter filter = new GraphApiKeyAuthenticationFilter();
        filter.setEnabled(true);
        filter.setApiKey("correct-key");
        filter.setHeaderName("X-Graph-API-Key");

        when(request.getRequestURI()).thenReturn("/api/v1/graph/ingest");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Graph-API-Key")).thenReturn("correct-key");

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }
}