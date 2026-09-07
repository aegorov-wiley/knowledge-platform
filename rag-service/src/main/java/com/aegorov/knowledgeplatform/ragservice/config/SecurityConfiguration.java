package com.aegorov.knowledgeplatform.ragservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Demo-grade role-based security for rag-service (WebFlux).
 * <p>
 * NOT production auth. Two in-memory users demonstrate an authorization layer:
 * <ul>
 *   <li>{@code user} / {@code user} — ROLE_USER</li>
 *   <li>{@code admin} / {@code admin} — ROLE_ADMIN</li>
 * </ul>
 * The UI, actuator health/info and OpenAPI are open; every {@code /api/v1/rag/**}
 * call must be authenticated, and the caller's username becomes the conversation
 * owner (replacing the former {@code X-User-Id} header). Passwords are
 * intentionally visible {@code {noop}} demo credentials.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                "/", "/index.html", "/favicon.ico",
                                "/css/**", "/js/**", "/webjars/**",
                                "/actuator/health/**", "/actuator/info", "/actuator/prometheus")
                        .permitAll()
                        .pathMatchers("/api/v1/rag/**").hasAnyRole("USER", "ADMIN")
                        .anyExchange().authenticated())
                .httpBasic(withDefaults())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }

    @Bean
    MapReactiveUserDetailsService userDetailsService() {
        UserDetails user = User.withUsername("user")
                .password("{noop}user")
                .roles("USER")
                .build();
        UserDetails admin = User.withUsername("admin")
                .password("{noop}admin")
                .roles("ADMIN")
                .build();
        return new MapReactiveUserDetailsService(user, admin);
    }
}
