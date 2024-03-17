package com.drunkenlion.alcoholfriday;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@OpenAPIDefinition(servers = {
		@Server(url = "https://api.alcoholfriday.store", description = "store"),
		@Server(url = "https://api.alcoholfriday.shop", description = "shop")
})
@SpringBootApplication
@EnableJpaAuditing
public class AlcoholfridayApplication {
	public static void main(String[] args) {
		SpringApplication.run(AlcoholfridayApplication.class, args);
	}
}
