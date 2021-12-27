package com.hst.study.reactive.tobyreactive.chapter4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dlgusrb0808@gmail.com
 */
public class LoadTest {

	private static final Logger logger = LoggerFactory.getLogger(LoadTest.class);

	static AtomicInteger couter = new AtomicInteger(0);

	public static void main(String[] args) throws InterruptedException {
		ExecutorService es = Executors.newFixedThreadPool(100);

		RestTemplate rt = new RestTemplate();
		String url = "http://localhost:8080/dr";

		StopWatch main = new StopWatch();
		main.start();

		for (int i = 0; i < 100; i++) {
			es.execute(() -> {
				int idx = couter.addAndGet(1);
				logger.info("Thread {}", idx);

				StopWatch sw = new StopWatch();
				sw.start();

				rt.getForObject(url, String.class);

				sw.stop();

				logger.info("Thread {} Elapsed : {}", idx, sw.getTotalTimeSeconds());
			});
		}

		es.shutdown();
		es.awaitTermination(100, TimeUnit.SECONDS);

		main.stop();
		logger.info("Total: {}", main.getTotalTimeSeconds());
	}

}
