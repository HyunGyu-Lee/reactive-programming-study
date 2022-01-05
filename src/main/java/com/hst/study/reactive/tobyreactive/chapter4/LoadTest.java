package com.hst.study.reactive.tobyreactive.chapter4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dlgusrb0808@gmail.com
 */
public class LoadTest {

	private static final Logger logger = LoggerFactory.getLogger(LoadTest.class);

	static AtomicInteger couter = new AtomicInteger(0);

	public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
		ExecutorService es = Executors.newFixedThreadPool(100);

		RestTemplate rt = new RestTemplate();
		String url = "http://localhost:8080/rest?idx={idx}";

		// 스레드 동기화
		CyclicBarrier barrier = new CyclicBarrier(101);

		StopWatch main = new StopWatch();
		main.start();

		for (int i = 0; i < 100; i++) {
			es.submit(() -> {
				int idx = couter.addAndGet(1);

				barrier.await();

				logger.info("Thread {}", idx);

				StopWatch sw = new StopWatch();
				sw.start();

				String res = rt.getForObject(url, String.class, idx);

				sw.stop();

				logger.info("Thread {} Elapsed : {}, Res: {}", idx, sw.getTotalTimeSeconds(), res);
				return null;
			});
		}

		barrier.await();

		es.shutdown();
		es.awaitTermination(100, TimeUnit.SECONDS);

		main.stop();
		logger.info("Total: {}", main.getTotalTimeSeconds());
	}

}
