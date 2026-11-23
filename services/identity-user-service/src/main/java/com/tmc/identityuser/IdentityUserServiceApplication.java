package com.tmc.identityuser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.context.annotation.Import(com.tmc.events.JdbcEventPublisher.class)
@SpringBootApplication
public class IdentityUserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityUserServiceApplication.class, args);
	}
}
