package com.mochu.framework.security;

import com.mochu.common.constant.Constants;
import com.mochu.common.security.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JWT 认证过滤器 — 对照 V3.2 §4.1.2
 * 从 Authorization 头提取 Bearer token，解析后设置 SecurityContext
 * Token 剩余 7 天时自动刷新，新 Token 通过 X-New-Token 响应头返回
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, Object> redisTemplate;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null) {
            try {
                Integer userId = jwtUtils.getUserId(token);
                String clientType = request.getHeader("X-Client-Type");
                if (clientType == null) {
                    clientType = "pc";
                }

                // Redis 校验 Token 有效性
                String redisKey = Constants.REDIS_TOKEN_PREFIX + userId + ":" + clientType;
                Object storedToken = redisTemplate.opsForValue().get(redisKey);
                if (storedToken == null || !token.equals(storedToken.toString())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // 构建 LoginUser
                String username = jwtUtils.getUsername(token);
                String permKey = Constants.REDIS_PERMISSIONS_PREFIX + userId;
                @SuppressWarnings("unchecked")
                Object permObj = redisTemplate.opsForValue().get(permKey);
                Set<String> permissions;
                if (permObj instanceof Set) {
                    permissions = (Set<String>) permObj;
                } else if (permObj instanceof Collection) {
                    permissions = new HashSet<>((Collection<String>) permObj);
                } else {
                    permissions = Set.of();
                }

                LoginUser loginUser = new LoginUser();
                loginUser.setUserId(userId);
                loginUser.setUsername(username);
                loginUser.setPermissions(permissions);

                // 从 Redis 加载 dataScope 和 deptId
                String userInfoKey = Constants.REDIS_TOKEN_PREFIX + "info:" + userId;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> userInfoMap =
                        (java.util.Map<String, Object>) redisTemplate.opsForValue().get(userInfoKey);
                if (userInfoMap != null) {
                    if (userInfoMap.get("deptId") instanceof Number n) {
                        loginUser.setDeptId(n.intValue());
                    }
                    if (userInfoMap.get("dataScope") instanceof Number n) {
                        loginUser.setDataScope(n.intValue());
                    }
                    if (userInfoMap.get("realName") != null) {
                        loginUser.setRealName(userInfoMap.get("realName").toString());
                    }
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Token 剩余 7 天自动刷新
                if (jwtUtils.shouldRefresh(token)) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = jwtUtils.parseToken(token).get("roles", List.class);
                    String newToken = jwtUtils.generateToken(userId, username, roles, clientType);
                    redisTemplate.opsForValue().set(redisKey, newToken,
                            Constants.TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
                    response.setHeader("X-New-Token", newToken);
                }
            } catch (Exception e) {
                log.debug("Token 解析失败: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
