package com.hst.study.reactive.tobyreactive.chapter1;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;

/**
 * @author dlgusrb0808@gmail.com
 */
public class PubSub {

	public static void main(String[] args) throws InterruptedException {
		Iterable<Integer> iter = Arrays.asList(1, 2, 3, 4, 5);
		ExecutorService es = Executors.newSingleThreadExecutor();

		Publisher<Integer> p = new Publisher<>() {
			@Override
			public void subscribe(Subscriber subscriber) {
				Iterator<Integer> it = iter.iterator();

				subscriber.onSubscribe(new Subscription() {
					@Override
					public void request(long n) {
						// Subscriber가 Purblisher한테 요청한 갯수가 전달됨
						// backpressure 라는 Publisher와 Subscriber간 처리량 차이를 조절할 수 있음
						// Subscriber의 onSubscribe 시 Subscription.request 메소드를 호출
						es.execute(() -> {
							int i = 0;
							try {
								while (i++ < n) {
									if (it.hasNext()) {
										subscriber.onNext(it.next());
									} else {
										subscriber.onComplete();
										break;
									}
								}
							} catch (Exception e) {
								subscriber.onError(e);
							}
						});
					}

					@Override
					public void cancel() {

					}
				});
			}
		};

		Subscriber<Integer> s = new Subscriber<>() {

			private Subscription subscription;

			@Override
			public void onSubscribe(Subscription subscription) {
				System.out.println("onSubscribe");
				this.subscription = subscription;
				subscription.request(1);
			}

			@Override
			public void onNext(Integer item) {
				System.out.println("onNext call " + item);
				subscription.request(1);
			}

			@Override
			public void onError(Throwable throwable) {
				System.out.println("onError");
			}

			@Override
			public void onComplete() {
				System.out.println("onComplete");
			}
		};

		p.subscribe(s);
		es.awaitTermination(10, TimeUnit.HOURS);
		es.shutdown();
	}

}
