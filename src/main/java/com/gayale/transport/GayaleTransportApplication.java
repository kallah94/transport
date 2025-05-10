package com.gayale.transport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class GayaleTransportApplication {

	public static void main(String[] args) {
		SpringApplication.run(GayaleTransportApplication.class, args);
	}

}
