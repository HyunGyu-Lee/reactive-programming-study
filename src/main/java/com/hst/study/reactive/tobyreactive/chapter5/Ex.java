package com.hst.study.reactive.tobyreactive.chapter5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.Future;

/**
 * @author dlgusrb0808@gmail.com
 */
@SpringBootApplication
public class Ex {

	@RestController
	public static class MyController {

		AsyncRestTemplate rt = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory());

		private static final Logger log = LoggerFactory.getLogger(MyController.class);

		@GetMapping("/rest")
		public DeferredResult<String> rest(int idx) {
			DeferredResult<String> dr = new DeferredResult<>();
			ListenableFuture<ResponseEntity<String>> res =
					rt.getForEntity("http://localhost:8081/service?req={req}", String.class, "hello" + idx);
			res.addCallback(s -> {
				dr.setResult(s.getBody() + "work");
			}, e -> {
			});
			return dr;
		}

	}

	public static void main(String[] args) {
		SpringApplication.run(Ex.class);
	}

}
