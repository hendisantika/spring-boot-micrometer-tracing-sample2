package com.hendisantika.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Flux;
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

interface CustomerHttpClient {
	@GetExchange("/customers")
	Flux<Customer> fetchCustomers();
}

record Customer(int id, String name) {
}

@Controller
@ResponseBody
class CustomerController {

	private final CustomerHttpClient client;
	Logger log = LoggerFactory.getLogger(CustomerController.class);

	CustomerController(CustomerHttpClient client) {
		this.client = client;
	}

	@GetMapping("/all")
	public Flux<Customer> all() {
		log.info("Fetching customers");
		return client.fetchCustomers();
	}
}
