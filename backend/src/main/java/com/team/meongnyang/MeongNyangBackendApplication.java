package com.team.meongnyang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MeongNyangBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeongNyangBackendApplication.class, args);
	}

}
