package org.xhy.infrastructure.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private static String jwtSecret = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    // token过期时间 - 24小时
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    private static SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** 生成JWT Token */
    public static String generateToken(String userId) {
        if (!StringUtils.hasText(userId)) {
            logger.error("生成JWT Token失败: 用户ID为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        String token = Jwts.builder().subject(userId).issuedAt(now).expiration(expiryDate).signWith(getSigningKey())
                .compact();

        logger.debug("成功生成JWT Token: 用户ID={}, 过期时间={}", userId, expiryDate);
        return token;
    }

    /** 从token中获取用户ID */
    public static String getUserIdFromToken(String token) {
        if (!StringUtils.hasText(token)) {
            logger.warn("获取用户ID失败: Token为空");
            return null;
        }

        try {
            Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

            String userId = claims.getSubject();
            Date expiration = claims.getExpiration();

            logger.debug("成功解析Token: 用户ID={}, 过期时间={}", userId, expiration);
            return userId;

        } catch (JwtException e) {
            logger.warn("解析Token失败: {}, Token前缀: {}", e.getMessage(),
                    token.length() > 20 ? token.substring(0, 20) + "..." : token);
            return null;
        } catch (Exception e) {
            logger.error("解析Token异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /** 验证token是否有效 */
    public static boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            logger.debug("Token验证失败: Token为空");
            return false;
        }

        try {
            Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

            // 检查是否过期
            Date expiration = claims.getExpiration();
            Date now = new Date();

            if (expiration.before(now)) {
                logger.warn("Token验证失败: Token已过期, 过期时间={}, 当前时间={}", expiration, now);
                return false;
            }

            logger.debug("Token验证成功: 用户ID={}, 剩余有效时间={}分钟", claims.getSubject(),
                    (expiration.getTime() - now.getTime()) / (1000 * 60));

            return true;

        } catch (JwtException e) {
            logger.warn("Token验证失败 - JWT异常: {}, Token前缀: {}", e.getMessage(),
                    token.length() > 20 ? token.substring(0, 20) + "..." : token);
            return false;
        } catch (Exception e) {
            logger.error("Token验证异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /** 检查Token是否即将过期（1小时内） */
    public static boolean isTokenExpiringSoon(String token) {
        if (!StringUtils.hasText(token)) {
            return true;
        }

        try {
            Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

            Date expiration = claims.getExpiration();
            Date now = new Date();

            // 检查是否在1小时内过期
            long timeUntilExpiry = expiration.getTime() - now.getTime();
            boolean expiringSoon = timeUntilExpiry < (60 * 60 * 1000); // 1小时

            if (expiringSoon) {
                logger.info("Token即将过期: 用户ID={}, 剩余时间={}分钟", claims.getSubject(), timeUntilExpiry / (1000 * 60));
            }

            return expiringSoon;

        } catch (Exception e) {
            logger.debug("检查Token过期状态失败: {}", e.getMessage());
            return true;
        }
    }
}