package com.tmc.qr.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.qr.client.EmployeeAccess;

@Configuration
public class SecurityConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnProperty(name = "tmc.internal.enabled", havingValue = "false", matchIfMissing = true)
    EmployeeAccess unavailableEmployeeAccess() {
        return (employeeId, factoryId) -> {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_UNAVAILABLE");
        };
    }

    @Bean
    SecurityFilterChain qrSecurity(HttpSecurity http, com.tmc.api.ApiSecurityHandlers apiSecurityHandlers) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/me/qr/**").hasAuthority("SCOPE_qr.issue")
                        .requestMatchers("/internal/qr/verifications", "/internal/qr/jwks").hasAuthority("SCOPE_qr.verify")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(apiSecurityHandlers).accessDeniedHandler(apiSecurityHandlers))
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(apiSecurityHandlers)
                        .accessDeniedHandler(apiSecurityHandlers).jwt(jwt -> { })).build();
    }
}
