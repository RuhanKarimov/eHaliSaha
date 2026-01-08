package com.ornek.ehalisaha.ehalisahabackend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth

                // ✅ STATIC/FRONT: tarayıcı <script>, <link> isteklerine header ekleyemez.
                // O yüzden UI dosyaları kesinlikle permitAll olmalı.
                .requestMatchers(
                        "/", "/index.html",
                        "/ui/**",
                        "/favicon.ico",
                        "/robots.txt",
                        "/.well-known/**"
                ).permitAll()

                // ✅ Swagger / docs / health
                .requestMatchers(
                        "/swagger-ui/**", "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/health"
                ).permitAll()

                // ✅ Public API
                .requestMatchers("/api/public/**").permitAll()

                // ✅ Role-protected API
                .requestMatchers("/api/owner/**").hasAuthority("ROLE_OWNER")
                .requestMatchers("/api/member/**").hasAuthority("ROLE_MEMBER")

                // ✅ Other API endpoints (including /api/me)
                .requestMatchers("/api/**").authenticated()

                // ✅ Any other request
                .anyRequest().permitAll()
        );

        http.httpBasic(basic -> basic.authenticationEntryPoint((req, res, ex) -> {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setHeader("WWW-Authenticate", ""); // browser popup kapansın
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("""
                    {"status":401,"error":"Unauthorized","message":"Authentication required"}
                    """);
        }));

        http.exceptionHandling(eh -> eh.accessDeniedHandler((req, res, ex) -> {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("""
                    {"status":403,"error":"Forbidden","message":"Access denied"}
                    """);
        }));

        return http.build();
    }
}
