package com.hst.study.reactive.tobyreactive.chapter4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author dlgusrb0808@gmail.com
 */
public class FutureEx {
	/***
	 * TODO
	 * TODO
	 * TODO
	 * https://www.youtube.com/watch?v=aSTuQiPB4Ns&list=PLOLeoJ50I1kkqC4FuEztT__3xKSfR2fpw&index=4
	 * 토비 리액티브(4) 자바와 스프의 비동기 기술 - 52.57 초까지 들었음
	 * TODO
	 * TODO
	 * TODO
	 * TODO
	 */
	private static final Logger logger = LoggerFactory.getLogger(FutureEx.class);

	interface SuccessCallback {
		void onSuccess(String result);
	}

	interface ExceptionCallback {
		void onError(Throwable t);
	}

	public static class CallableFutureTask extends FutureTask<String> {
		SuccessCallback callback;
		ExceptionCallback ec;

		public CallableFutureTask(Callable<String> callable, SuccessCallback callback, ExceptionCallback ec) {
			super(callable);
			this.callback = Objects.requireNonNull(callback);
			this.ec = Objects.requireNonNull(ec);
		}

		@Override
		protected void done() {
			try {
				callback.onSuccess(get());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				ec.onError(e);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		logger.info("Start Program");
		ExecutorService es = Executors.newCachedThreadPool();
		CallableFutureTask f = new CallableFutureTask(() -> {
			Thread.sleep(2000);
			logger.info("In Async");
			return "Hello";
		}, System.out::println, System.err::println);
		es.execute(f);
		logger.info("Exit");
		es.shutdown();

	}

}
