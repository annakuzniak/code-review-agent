package com.annakuzniak.code_review_agent.webhook;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureFilter.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String webhookSecret;

    public WebhookSignatureFilter(@Value("${github.webhook-secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().equals("/webhook/github")) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] bodyBytes = request.getInputStream().readAllBytes();
        String payload = new String(bodyBytes, StandardCharsets.UTF_8);

        String signature = request.getHeader("X-Hub-Signature-256");

        if (signature == null || signature.isBlank()) {
            log.warn("Rejected webhook request: missing X-Hub-Signature-256 header");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing signature");
            return;
        }

        if (!isValidSignature(payload, signature)) {
            log.warn("Rejected webhook request: invalid signature");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
            return;
        }

        log.info("Webhook signature validated successfully");

        // Wrap request so the body can be read again downstream
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bodyBytes);
                return new ServletInputStream() {
                    @Override
                    public boolean isFinished() {
                        return byteArrayInputStream.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener readListener) {
                    }

                    @Override
                    public int read() {
                        return byteArrayInputStream.read();
                    }
                };
            }
        };

        filterChain.doFilter(wrappedRequest, response);
    }

    private boolean isValidSignature(String payload, String githubSignature) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = "sha256=" + bytesToHex(hash);

            return MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    githubSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}