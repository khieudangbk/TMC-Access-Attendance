package com.tmc.gatestation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain stationSecurity(HttpSecurity http, com.tmc.api.ApiSecurityHandlers apiSecurityHandlers) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/internal/stations/*/checks").hasAuthority("SCOPE_stations.verify")
                        .requestMatchers(HttpMethod.POST, "/stations/*/heartbeat").hasAuthority("SCOPE_access.scan")
                        .requestMatchers("/stations/**", "/factories/**").hasAuthority("SCOPE_stations.manage")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(apiSecurityHandlers).accessDeniedHandler(apiSecurityHandlers))
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(apiSecurityHandlers)
                        .accessDeniedHandler(apiSecurityHandlers).jwt(jwt -> { })).build();
    }
}
