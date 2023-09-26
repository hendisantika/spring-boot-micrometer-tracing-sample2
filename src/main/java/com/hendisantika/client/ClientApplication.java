package com.hendisantika.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class ClientApplication {
Hooks.enableAutomaticContextPropagation();
	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}

	@Bean
	CustomerHttpClient customerHttpClient(WebClient.Builder builder) {
		return HttpServiceProxyFactory
				.builder()
				.clientAdapter(WebClientAdapter.forClient(builder
						.baseUrl("http://localhost:9090")
						.build()
				))
				.build()
				.createClient(CustomerHttpClient.class);
	}
}
