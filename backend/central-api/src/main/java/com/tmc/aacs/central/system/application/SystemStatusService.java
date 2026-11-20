package com.tmc.aacs.central.system.application;

import java.time.Clock;
import java.time.Instant;

import com.tmc.aacs.central.system.application.port.out.DatabaseProbe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SystemStatusService {

    private final DatabaseProbe databaseProbe;
    private final Clock clock;
    private final String application;
    private final String version;
    private final String environment;

    public SystemStatusService(
            DatabaseProbe databaseProbe,
            Clock clock,
            @Value("${spring.application.name}") String application,
            @Value("${info.app.version:dev}") String version,
            @Value("${app.environment:local}") String environment) {
        this.databaseProbe = databaseProbe;
        this.clock = clock;
        this.application = application;
        this.version = version;
        this.environment = environment;
    }

    public SystemStatus current() {
        return new SystemStatus(
                application,
                version,
                environment,
                Instant.now(clock),
                databaseProbe.isUp() ? "UP" : "DOWN");
    }

    public record SystemStatus(
            String application,
            String version,
            String environment,
            Instant serverTime,
            String database
    ) {
    }
}
