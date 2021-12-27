package com.hst.study.reactive.tobyreactive.chapter4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

/**
 * @author dlgusrb0808@gmail.com
 */
@SpringBootApplication
@EnableAsync
public class AsyncWithSpring {

	private static final Logger logger = LoggerFactory.getLogger(AsyncWithSpring.class);

	@RestController
	public static class MyController {
		Queue<DeferredResult<String>> results = new ConcurrentLinkedQueue<>();

		@GetMapping("sync")
		public String sync() throws InterruptedException {
			Thread.sleep(2000);
			return "hello";
		}

		@GetMapping("/async")
		public Callable<String> async() throws InterruptedException {
			return () -> {
				Thread.sleep(2000);
				return "hello";
			};
		}

		@GetMapping("/dr")
		public DeferredResult<String> deferredResult() {
			DeferredResult<String> dr = new DeferredResult<>(600000L);
			results.add(dr);
			return dr;
		}

		@GetMapping("/dr/count")
		public String drCount() {
			return String.valueOf(results.size());
		}

		@GetMapping("/dr/event")
		public String drEvent(String msg) {
			for (DeferredResult<String> dr :results) {
				dr.setResult("Hello " + msg);
				results.remove(dr);
			}
			return "OK";
		}

		@GetMapping("/emitter")
		public ResponseBodyEmitter emitter() {
			ResponseBodyEmitter emitter = new ResponseBodyEmitter();
			Executors.newSingleThreadExecutor().execute(() -> {
				for (int i = 0; i <= 50; i++) {
					try {
						emitter.send("<p> Stream " + i + "</p>");
						Thread.sleep(1000);
					} catch (Exception e) {}
				}
			});
			return emitter;
		}

	}

	@Bean
	public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setCorePoolSize(20);
		threadPoolTaskExecutor.setMaxPoolSize(50);
		threadPoolTaskExecutor.setQueueCapacity(100);
		return threadPoolTaskExecutor;
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext c = SpringApplication.run(AsyncWithSpring.class, args);
	}

}
