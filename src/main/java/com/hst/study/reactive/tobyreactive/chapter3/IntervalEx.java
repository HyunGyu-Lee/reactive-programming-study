package com.hst.study.reactive.tobyreactive.chapter3;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dlgusrb0808@gmail.com
 */
public class IntervalEx {

	private static final Logger log = LoggerFactory.getLogger(IntervalEx.class);

	public static void main(String[] args) {
		Publisher<Integer> pub = sub -> sub.onSubscribe(new Subscription() {
			int no = 0;
			boolean cancelled = false;

			@Override
			public void request(long n) {
				ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
				exec.scheduleAtFixedRate(() -> {
					if (cancelled) {
						exec.shutdown();
						return;
					}
					sub.onNext(no++);
				}, 0, 1000, TimeUnit.MILLISECONDS);
			}

			@Override
			public void cancel() {
				this.cancelled = true;
			}
		});

		Publisher<Integer> takePub = new Publisher<>() {
			@Override
			public void subscribe(Subscriber<? super Integer> sub) {
				pub.subscribe(new Subscriber<Integer>() {
					int count = 0;
					Subscription subscription;

					@Override
					public void onSubscribe(Subscription s) {
						sub.onSubscribe(s);
						this.subscription = s;
					}

					@Override
					public void onNext(Integer integer) {
						if (++count > 5) {
							subscription.cancel();
						}
						sub.onNext(integer);
					}

					@Override
					public void onError(Throwable t) {
						sub.onError(t);
					}

					@Override
					public void onComplete() {
						sub.onComplete();
					}
				});
			}
		};

		takePub.subscribe(new Subscriber<>() {
			@Override
			public void onSubscribe(Subscription s) {
				log.info("onSubscribe");
				s.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Integer integer) {
				log.info("onNext: {}", integer);
			}

			@Override
			public void onError(Throwable t) {
				log.info("onError: {}", t);
			}

			@Override
			public void onComplete() {
				log.info("onComplete");
			}
		});
	}

}
