package com.evdealer.evdealermanagement.configurations;

import com.evdealer.evdealermanagement.service.implement.AccountDetailsService;
import com.evdealer.evdealermanagement.service.implement.JwtService;
import com.evdealer.evdealermanagement.service.implement.RedisService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final AccountDetailsService userDetailsService;
    private final RedisService redisService;

    // ===== THÊM DANH SÁCH PUBLIC ENDPOINTS =====
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/auth/",
            "/oauth2/",
            "/product/",
            "/vehicle/",
            "/battery/",
            "/api/vnpayment/return",
            "/api/vnpayment/ipn",
            "/gemini/suggest-price",
            "/api/webhooks/eversign/document-complete",
            "/api/password/",
            "/profile/public",
            "/member/product/seller",
            "/swagger-ui",
            "/swagger-ui/",
            "/swagger-ui/index.html",
            "/swagger-ui/swagger-initializer.js",
            "/swagger-ui/swagger-ui.css",
            "/swagger-ui/swagger-ui-bundle.js",
            "/v3/api-docs",
            "/v3/api-docs/",
            "/v3/api-docs/swagger-config",
            "/public/brands"
    );


    public JwtAuthenticationFilter(JwtService jwtService, AccountDetailsService userDetailsService, RedisService redisService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        logger.debug("Processing request: {} {}", request.getMethod(), requestPath);

        // ===== 1. KIỂM TRA PUBLIC ENDPOINT - BỎ QUA JWT VALIDATION =====
        if (isPublicEndpoint(requestPath)) {
            logger.debug("Public endpoint detected, skipping JWT validation: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // ===== 2. LẤY TOKEN TỪ HEADER =====
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No Bearer token found for protected endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            logger.debug("Empty token provided");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // ===== 3. KIỂM TRA BLACKLIST (REDIS) =====
            try {
                if (redisService.isBlacklisted(token)) {
                    logger.warn("Blacklisted token detected");
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Token has been blacklisted\"}");
                    return; // DỪNG NGAY, KHÔNG CHO QUA
                }
            } catch (Exception redisEx) {
                logger.warn("Redis unavailable, skipping blacklist check: {}", redisEx.getMessage());
            }

            // ===== 4. EXTRACT USERNAME TỪ TOKEN =====
            String username = jwtService.extractUsername(token);

            if (username == null) {
                logger.warn("Cannot extract username from token");
                filterChain.doFilter(request, response);
                return;
            }

            // ===== 5. KIỂM TRA XEM ĐÃ AUTHENTICATED CHƯA =====
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                logger.debug("User already authenticated: {}", username);
                filterChain.doFilter(request, response);
                return;
            }

            // ===== 6. LOAD USER DETAILS =====
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // ===== 7. VALIDATE TOKEN =====
            if (!jwtService.validateToken(token, userDetails)) {
                logger.warn("Invalid token for user: {}", username);
                filterChain.doFilter(request, response);
                return;
            }

            // ===== 8. EXTRACT ROLES TỪ TOKEN =====
            Claims claims = jwtService.extractAllClaims(token);
            List<String> roles = claims.get("roles", List.class);

            // SỬA: Xử lý null-safe
            List<SimpleGrantedAuthority> authorities;
            if (roles != null && !roles.isEmpty()) {
                authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                logger.debug("Roles from token: {}", roles);
            } else {
                // Fallback to userDetails authorities
                authorities = userDetails.getAuthorities().stream()
                        .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))
                        .collect(Collectors.toList());
                logger.debug("Using authorities from UserDetails");
            }

            // ===== 9. TẠO AUTHENTICATION TOKEN =====
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    authorities
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // ===== 10. SET AUTHENTICATION VÀO SECURITY CONTEXT =====
            SecurityContextHolder.getContext().setAuthentication(authToken);
            logger.debug("User authenticated successfully: {} with authorities: {}",
                    username, authToken.getAuthorities());

        } catch (BadCredentialsException e) {
            logger.error("Bad credentials: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            // KHÔNG gọi filterChain.doFilter() → Sẽ bị chặn bởi Spring Security
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid credentials\"}");
            return;

        } catch (Exception e) {
            logger.error("JWT filter error for path {}: {}", requestPath, e.getMessage(), e);
            SecurityContextHolder.clearContext();
            // Tiếp tục filter chain để Spring Security xử lý
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Kiểm tra xem request có phải là public endpoint không
     */
    private boolean isPublicEndpoint(String requestPath) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(publicPath ->
                requestPath.startsWith(publicPath) ||
                        requestPath.equals(publicPath.replaceAll("/$", "")) // chấp cả khi thiếu dấu '/'
        );
    }

}