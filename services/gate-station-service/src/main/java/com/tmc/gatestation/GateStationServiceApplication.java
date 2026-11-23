package com.tmc.gatestation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.context.annotation.Import(com.tmc.events.JdbcEventPublisher.class)
@SpringBootApplication
public class GateStationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GateStationServiceApplication.class, args);
	}
}
