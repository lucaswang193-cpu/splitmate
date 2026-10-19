package com.splitmate.security;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.splitmate.entity.User;
import com.splitmate.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    // Filter method to intercept incoming requests and authenticate the user based on the JWT token
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token);
        }

        filterChain.doFilter(request, response);
    }

    // Helper method to extract the JWT token from the Authorization header of the request
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length());
    }

    // Helper method to authenticate the user based on the JWT token and set the authentication in the security context
    private void authenticate(String token) {
        try {
            jwtTokenProvider.isValid(token);
            UUID userId = jwtTokenProvider.getUserId(token);

            userRepository.findById(userId)
                    .ifPresent(this::setAuthentication);
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    // Helper method to set the authentication in the security context for the authenticated user
    private void setAuthentication(User user) {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);

        // Create an authentication token with the authenticated user and set it in the security context
        // 1. Principle 2. Credentials 3. Authorities
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
