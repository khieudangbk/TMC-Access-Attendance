package com.tmc.aacs.edge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Edge Access Agent: runs inside one TMC factory and owns the scan path.
 * The scan pipeline (C05-C11) must never call the central database synchronously.
 */
@SpringBootApplication
public class EdgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdgeApplication.class, args);
    }
}
