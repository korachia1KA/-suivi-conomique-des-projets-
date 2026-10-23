package com.example.project.config;

import com.example.project.service.UserService;
import com.example.project.service.auth.RefreshTokenService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security filter that intercepts incoming HTTP requests once per request.
 * Extracts and validates JWT tokens, checks active sessions, and sets user authentication in the SecurityContext.
 */

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final UserService userService;

    private final JwtTokenUtil jwtTokenUtil;

    private final RefreshTokenService refreshTokenService;

    public SecurityFilter(UserService userService, JwtTokenUtil jwtTokenUtil, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String path = request.getServletPath();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        if (path.startsWith("/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        final String header = request.getHeader("Authorization");

        String email = null;
        String jwtToken = null;
        String jti = null;

        if (header != null && header.startsWith("Bearer ")) {
            jwtToken = header.substring(7);
            try {
                email = jwtTokenUtil.getUsernameFromToken(jwtToken);
                jti = jwtTokenUtil.getJtiFromToken(jwtToken);
            } catch (IllegalArgumentException | ExpiredJwtException e) {
                chain.doFilter(request, response);
                return;
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userService.loadUserByUsername(email);

            if (jwtTokenUtil.validateToken(jwtToken, userDetails) && refreshTokenService.isSessionActive(jti)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}