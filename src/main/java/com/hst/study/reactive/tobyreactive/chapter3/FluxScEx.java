package com.hst.study.reactive.tobyreactive.chapter3;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * @author dlgusrb0808@gmail.com
 */
public class FluxScEx {

	private static final Logger log = LoggerFactory.getLogger(FluxScEx.class);

	public static void main(String[] args) throws InterruptedException {
		// 스레드는 user thread, daemon thread 가 있다.
		// JVM 은 daemon thread만 남아있으면 그대로 종료시킨다.
		// user thread 는 1개라도 남아있으면 종료시키지 않는다.

		// 아래 예제는 기본적으로 데몬 스레드로 만들기 때문에 별도로 슬립을 걸어주지 않으면 실행되지 않는다.
		Flux.interval(Duration.ofMillis(200))
			.take(10)
			.subscribe(e -> log.info("{}", e));
		log.info("exit");

		// 위 예시를 실행시키기 위해 sleep 걸어줌
		TimeUnit.SECONDS.sleep(5);
	}

}
