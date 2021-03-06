package com.hst.study.reactive.tobyreactive.chapter5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author dlgusrb0808@gmail.com
 */
@SpringBootApplication
public class RemoteService {

	@RestController
	public static class MyController {

		@GetMapping("/service")
		public String service(String req) throws InterruptedException {
			Thread.sleep(2000);
			return req + "/service";
		}

		@GetMapping("/error-service")
		@ResponseStatus(HttpStatus.NOT_FOUND)
		public String errorService(String req) throws InterruptedException {
			Thread.sleep(2000);
			return req + "/service";
		}
	}

	public static void main(String[] args) {
		System.setProperty("server.port", "8081");
		System.setProperty("server.tomcat.max-threads", "1000");
		SpringApplication.run(RemoteService.class);
	}

}
