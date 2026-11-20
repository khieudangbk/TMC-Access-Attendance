package com.tmc.aacs.central.architecture;

import com.tmc.aacs.central.CentralApplication;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Fails the build when a domain module imports another module's internals or a cycle appears.
 * This is the executable form of AR-04 (docs/03-decisions/ADR-008-central-modular-monolith.md).
 */
class ModularityTests {

    @Test
    void verifiesModuleBoundaries() {
        ApplicationModules modules = ApplicationModules.of(CentralApplication.class);
        assertFalse(modules.stream().toList().isEmpty(), "expected at least one application module");
        modules.verify();
    }
}
