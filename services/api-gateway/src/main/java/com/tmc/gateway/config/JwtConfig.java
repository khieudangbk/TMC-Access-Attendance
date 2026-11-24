package com.tmc.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/** JWT conversion is separate from route authorization and CORS configuration. */
@Configuration
public class JwtConfig {

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        // Keep the OAuth2 scope -> SCOPE_* convention of the TMC Authorization Server.
        return new JwtAuthenticationConverter();
    }
}
