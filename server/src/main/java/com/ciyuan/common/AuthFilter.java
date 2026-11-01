package com.ciyuan.common;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    Claims claims = jwtUtil.parse(header.substring(7));
                    UserContext.set(Long.valueOf(claims.getSubject()), String.valueOf(claims.get("role")));
                } catch (Exception ignore) {
                    // invalid token -> treated as anonymous
                }
            }
            chain.doFilter(req, resp);
        } finally {
            UserContext.clear();
        }
    }
}
