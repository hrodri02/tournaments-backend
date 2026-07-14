package com.example.tournaments_backend.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the client IP used to key rate-limit buckets: the first entry of
 * {@code X-Forwarded-For} when present, otherwise {@code request.getRemoteAddr()}.
 *
 * <p>This trusts the first XFF entry because the deployment fronts the app with an
 * nginx reverse proxy that overwrites the header with the real connecting IP.
 */
@Component
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    public String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (StringUtils.hasText(forwardedFor)) {
            String first = forwardedFor.split(",", 2)[0].trim();
            if (StringUtils.hasText(first)) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }
}
