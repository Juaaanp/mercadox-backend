package com.IngSoftwarelll.mercadox.config;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.IngSoftwarelll.mercadox.security.JwtBearerFilter;
import com.IngSoftwarelll.mercadox.security.PublicPaths;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;

@EnableMethodSecurity(prePostEnabled = true)
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Value("${jwt.secret}")
    private String base64Secret;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        
        return http
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .csrf(csrf -> {
                        csrf.disable();
                        csrf.ignoringRequestMatchers("/api/webhooks/**");
                    }) //Put csrf for refresh token endpoint
                    .authorizeHttpRequests(auth -> auth.requestMatchers(PublicPaths.PATHS).permitAll().anyRequest().authenticated())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) //Spring Security will never create an HttpSession and it will never use it to obtain the Security Context.
                    .addFilterBefore(new JwtBearerFilter(base64Secret), UsernamePasswordAuthenticationFilter.class) //Filter to extract JWT from cookies and set authentication in the security context.
                    .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                    .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-Token", "X-Client-Type"));
        configuration.setAllowCredentials(true); // Necesario para cookies
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
            writeErrorResponse(request, response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Invalid or expired JWT");
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
            writeErrorResponse(request, response,
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "Insufficient permissions");
    }

        private void writeErrorResponse(HttpServletRequest request,
                                    HttpServletResponse response,
                                    int status,
                                    String error,
                                    String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());

        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
        response.getWriter().write(json);
    }
}
