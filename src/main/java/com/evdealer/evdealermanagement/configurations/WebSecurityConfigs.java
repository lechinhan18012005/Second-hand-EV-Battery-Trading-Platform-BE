package com.evdealer.evdealermanagement.configurations;

import com.evdealer.evdealermanagement.service.implement.AccountDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.SecureRandom;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfigs {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AccountDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CustomOAuth2SuccessHandler successHandler;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:5175",
                "http://localhost:5185",
                "http://localhost:4173",
                "http://127.0.0.1:5500",
                "https://eco-green-p80o.onrender.com",
                "https://eco-green.store",
                "api-eco-green-be.huanops.com", "https://d3k8h5w5waqdh2.cloudfront.net"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Các endpoint công khai (public)
                        .requestMatchers(
                                "/ws-notifications/**",
                                "/auth/**", "/oauth2/**", "/login/oauth2/**",
                                "/vehicle/**", "/battery/**",
                                "/product/**", "/gemini/**",
                                "/api/password/**",
                                "/profile/public/**", // 👈 phải đặt ở đây, TRƯỚC /profile/**
                                "/api/vnpayment/**",
                                "/api/momo/**",
                                "/api/webhooks/eversign/document-complete",
                                "/member/product/seller/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/seller-reviews/seller/**",
                                "/public/brands/**")
                        .permitAll()

                        // Các endpoint yêu cầu role
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**", "/revenue/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/member/**", "/profile/**", "/password/**")
                        .hasAnyRole("MEMBER", "ADMIN", "STAFF")

                        // Còn lại bắt buộc phải login
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        }))
                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler)
                        .failureHandler((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"" + ex.getMessage() + "\"}");
                        }))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
