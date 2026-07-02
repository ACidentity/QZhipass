package org.microsoft.qintelipass.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.microsoft.qintelipass.models.User;
import org.microsoft.qintelipass.security.AuthenticatedUser;
import org.microsoft.qintelipass.services.UserService;
import org.microsoft.qintelipass.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final UserService userService;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith(JwtUtil.BEARER_PREFIX)) {
            jwt = authorizationHeader.substring(JwtUtil.BEARER_PREFIX.length());

            try {
                if (jwtUtil.validateToken(jwt)) {
                    Long userId = null;
                    try {
                        userId = jwtUtil.extractUserId(jwt);
                    } catch (Exception e) {
                        log.warn("Could not extract user ID from token, will try to find by username");
                    }

                    String username = jwtUtil.extractUsername(jwt);
                    User user = null;

                    if (userId != null) {
                        user = userService.getUserById(userId);
                    }

                    if (user == null) {
                        user = userService.findByUsername(username);
                    }

                    if (user != null) {
                        AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
                                .userId(user.getId())
                                .username(user.getName())
                                .password(user.getPasswordHash())
                                .build();

                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        log.debug("User authenticated: userId={}, username={}", user.getId(), user.getName());
                    }
                }
            } catch (Exception e) {
                log.info("Token validation failed: {}", e.getMessage());
            }
        }

        if (filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }
}