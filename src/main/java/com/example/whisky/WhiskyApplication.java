package com.example.whisky;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class WhiskyApplication {

	public static void main(String[] args) {
		SpringApplication.run(WhiskyApplication.class, args);
	}

}
