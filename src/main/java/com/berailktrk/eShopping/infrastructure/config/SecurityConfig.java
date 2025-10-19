package com.berailktrk.eShopping.infrastructure.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.berailktrk.eShopping.infrastructure.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

// Spring Security Configuration - JWT authentication ve role-based authorization
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize kullanımı için
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Swagger/OpenAPI endpoints
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/error").permitAll()
                
                // Product public endpoints (ürün görüntüleme)
                .requestMatchers("/api/products", "/api/products/*", "/api/products/sku/*", "/api/products/search").permitAll()
                
                // Actuator endpoints (optional)
                .requestMatchers("/actuator/**").permitAll()
                
                // User endpoints - authentication gerektirir (cart, orders, payments, etc.)
                .requestMatchers("/api/cart/**").authenticated()
                .requestMatchers("/api/orders/**").authenticated()
                .requestMatchers("/api/payments/**").authenticated()
                
                // Admin endpoints - authentication gerektirir, role kontrolü @PreAuthorize ile
                .requestMatchers("/api/products/admin/**").authenticated()
                .requestMatchers("/api/admin/**").authenticated()
                .requestMatchers("/api/inventory/**").authenticated() // Tüm inventory endpoint'leri admin only
                
                // Tüm diğer endpoint'ler authentication gerektirir
                .anyRequest().authenticated()
            )
            // Exception handling - 401 ve 403 hataları için inline handler'lar
            .exceptionHandling(exception -> exception
                // 401 Unauthorized - Token eksik/geçersiz
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("text/plain; charset=UTF-8");
                    response.getWriter().write(
                        "401 Unauthorized - Bu endpoint için kimlik doğrulama gereklidir. " +
                        "Lütfen geçerli bir JWT token ile istek gönderin. Endpoint: " + request.getRequestURI()
                    );
                })
                // 403 Forbidden - Yetki yok
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("text/plain; charset=UTF-8");
                    response.getWriter().write(
                        "403 Forbidden - Bu işlem için yetkiniz bulunmamaktadır. " +
                        "Sadece ADMIN rolüne sahip kullanıcılar bu endpoint'e erişebilir. Endpoint: " + request.getRequestURI()
                    );
                })
            )
            // JWT filter'ı ekle
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
