package com.mochu.framework.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochu.common.result.R;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * HMAC-SHA256 签名验证过滤器
 * P5: 敏感操作需在请求头携带 X-Signature
 * 签名内容 = method + uri + timestamp + requestId
 */
@Slf4j
@Component
public class SignatureVerificationFilter implements Filter {

    @Value("${security.signature-key:MochuOA_Signature_Key_2026}")
    private String signatureKey;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 需要签名验证的路径 */
    private static final Set<String> SIGNED_PATHS = Set.of(
            "/api/v1/auth/login-by-password",
            "/api/v1/auth/login-by-sms",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/contracts",
            "/api/v1/payment/apply",
            "/api/v1/admin/users/reset-password",
            "/api/v1/finance/payments",
            "/api/v1/finance/receipts",
            "/api/v1/attachments",
            "/api/v1/hr/salary-config",
            "/api/v1/hr/tax-rate",
            "/api/v1/completion/labor"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        String method = req.getMethod().toUpperCase();

        // 仅校验匹配的路径 + POST/PUT/PATCH
        boolean needSign = SIGNED_PATHS.stream().anyMatch(uri::startsWith)
                && Set.of("POST", "PUT", "PATCH").contains(method);

        if (!needSign) {
            chain.doFilter(request, response);
            return;
        }

        // #N9 fix: 包装请求以支持多次读取 body（签名验证 + Controller）
        ContentCachingRequestWrapper wrappedReq = (req instanceof ContentCachingRequestWrapper)
                ? (ContentCachingRequestWrapper) req
                : new ContentCachingRequestWrapper(req);

        // #3 fix: 统一前后端签名协议 — 使用前端的格式
        // 前端: X-Sign / X-Timestamp / X-Nonce / Hex编码 / 包含body
        String signature = wrappedReq.getHeader("X-Sign");
        String timestamp = wrappedReq.getHeader("X-Timestamp");
        String nonce = wrappedReq.getHeader("X-Nonce");

        if (signature == null || timestamp == null) {
            writeError(resp, 400, "敏感操作需要签名验证");
            return;
        }

        // 校验时间戳（5分钟内有效）
        try {
            long ts = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() - ts) > 5 * 60 * 1000) {
                writeError(resp, 400, "签名已过期");
                return;
            }
        } catch (NumberFormatException e) {
            writeError(resp, 400, "时间戳格式无效");
            return;
        }

        // Nonce 防重放校验
        if (nonce == null || nonce.isBlank()) {
            writeError(resp, 400, "缺少Nonce参数");
            return;
        }
        String nonceKey = "nonce:" + nonce;
        Boolean setSuccess = stringRedisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", Duration.ofMinutes(5));
        if (!Boolean.TRUE.equals(setSuccess)) {
            writeError(resp, 400, "请求已处理（重复Nonce）");
            return;
        }

        // #N9 fix: 读取请求体参与签名，与前端一致: method\nurl\ntimestamp\nnonce\nbody
        // 先让 ContentCachingRequestWrapper 缓存 body
        wrappedReq.getInputStream().readAllBytes();
        String body = new String(wrappedReq.getContentAsByteArray(), StandardCharsets.UTF_8);
        String payload = method + "\n" + uri + "\n" + timestamp + "\n"
                + (nonce != null ? nonce : "") + "\n" + body;
        String expected = hmacSha256Hex(payload, signatureKey);
        if (!expected.equals(signature)) {
            writeError(resp, 400, "签名验证失败");
            return;
        }

        chain.doFilter(wrappedReq, response);
    }

    /**
     * #3 fix: 输出 Hex 编码以匹配前端 CryptoJS.HmacSHA256().toString(Hex)
     */
    private String hmacSha256Hex(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("签名计算失败", e);
        }
    }

    private void writeError(HttpServletResponse resp, int code, String msg)
            throws IOException {
        resp.setStatus(code);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(R.fail(code, msg)));
    }
}
