package com.hst.study.reactive.tobyreactive.chapter3;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dlgusrb0808@gmail.com
 */
public class SchedulerEx {

	private static final Logger log = LoggerFactory.getLogger(SchedulerEx.class);

	public static void main(String[] args) {
		Publisher<Integer> pub = (sub) -> {
			sub.onSubscribe(new Subscription() {
				@Override
				public void request(long n) {
					log.debug("request()");
					sub.onNext(1);
					sub.onNext(2);
					sub.onNext(3);
					sub.onNext(4);
					sub.onNext(5);
					sub.onComplete();
				}

				@Override
				public void cancel() {

				}
			});
		};

		// Publisher<Integer> subOnPub = (sub) -> {
		// 	ExecutorService es = Executors.newSingleThreadExecutor(new ThreadFactory() {
		// 		@Override
		// 		public Thread newThread(Runnable runnable) {
		// 			Thread thread = new Thread(runnable);
		// 			thread.setName("subOn-" + thread.getName());
		// 			return thread;
		// 		}
		// 	});
		// 	es.execute(() -> {
		// 		pub.subscribe(sub);
		// 	});
		// };

		Publisher<Integer> pubOnSub = sub -> {
			pub.subscribe(new Subscriber<Integer>() {
				ExecutorService es = Executors.newSingleThreadExecutor(new ThreadFactory() {
					@Override
					public Thread newThread(Runnable runnable) {
						Thread thread = new Thread(runnable);
						thread.setName("pubOn-" + thread.getName());
						return thread;
					}
				});

				@Override
				public void onSubscribe(Subscription s) {
					sub.onSubscribe(s);
				}

				@Override
				public void onNext(Integer integer) {
					es.execute(() -> {
						sub.onNext(integer);
					});
				}

				@Override
				public void onError(Throwable t) {
					es.execute(() -> {
						sub.onError(t);
					});
					es.shutdown();
				}

				@Override
				public void onComplete() {
					es.execute(() -> {
						sub.onComplete();
					});
					es.shutdown();
				}
			});
		};


		pubOnSub.subscribe(new Subscriber<>() {
			@Override
			public void onSubscribe(Subscription s) {
				log.debug("onSubscribe");
				s.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Integer i) {
				log.debug("onNext: {}", i);
			}

			@Override
			public void onError(Throwable t) {
				log.debug("onError: {}", t);
			}

			@Override
			public void onComplete() {
				log.debug("onComplete");
			}
		});

		log.debug("exit");
	}

}
