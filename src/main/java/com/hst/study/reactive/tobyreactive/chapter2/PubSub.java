package com.hst.study.reactive.tobyreactive.chapter2;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author dlgusrb0808@gmail.com
 */
public class PubSub {

	public static void main(String[] args) {
		Publisher<Integer> pub = iterPub(Stream.iterate(1, e -> e + 1).limit(10).collect(Collectors.toList()));
		Publisher<String> mapPub = mapPub(pub, e -> "[" + e + "]");
		Publisher<List<String>> mapPub2 = mapPub(pub, e -> Collections.singletonList("[" + e + "]"));
		Publisher<String> reducePub = reducePub(pub, "", (a, b) -> a + "-" + b);
		Publisher<StringBuilder> reducePub2 = reducePub(pub, new StringBuilder(), (a, b) -> a.append(b).append(","));
		mapPub.subscribe(logSub());
		mapPub2.subscribe(logSub());
		reducePub.subscribe(logSub());
		reducePub2.subscribe(logSub());
	}

	/**
	 * BiFunction<T, U, R>
	 * 메소드 시그니쳐: R apply(T t, U u)
	 * Function<T, R>이 인자 1개를 받아 1개를 반환할 때 어떤 연산을 수행할지를 표현하는 함수라면
	 * BiFunction<T, U, R>은 인자 2개를 받아 1개를 반환할 때 어떤 연산을 수행할지를 표현하는 함수이다.
	 * reduce 작업에 많이 쓰인다.
	 */
	private static <T, R> Publisher<R> reducePub(Publisher<T> publisher, R initialValue, BiFunction<R, T, R> reduceFunction) {
		return new Publisher<>() {
			@Override
			public void subscribe(Subscriber<? super R> subscriber) {
				publisher.subscribe(new DelegateSubscriber<T, R>(subscriber) {
					private R accumulatedValue = initialValue;

					@Override
					public void onNext(T data) {
						accumulatedValue = reduceFunction.apply(accumulatedValue, data);
					}

					@Override
					public void onComplete() {
						subscriber.onNext(accumulatedValue);
						subscriber.onComplete();
					}
				});
			}
		};
	}

	private static <T, R> Publisher<R> mapPub(Publisher<T> publisher, Function<T, R> mappingFunc) {
		return new Publisher<R>() {
			@Override
			public void subscribe(Subscriber<? super R> subscriber) {
				publisher.subscribe(new DelegateSubscriber<T, R>(subscriber) {
					@Override
					public void onNext(T integer) {
						subscriber.onNext(mappingFunc.apply(integer));
					}
				});
			}
		};
	}

	private static Publisher<Integer> iterPub(Iterable<Integer> iter) {
		return new Publisher<>() {
			@Override
			public void subscribe(Subscriber<? super Integer> subscriber) {
				subscriber.onSubscribe(new Subscription() {
					@Override
					public void request(long n) {
						try {
							iter.forEach(subscriber::onNext);
							subscriber.onComplete();
						} catch (Exception e) {
							subscriber.onError(e);
						}
					}

					@Override
					public void cancel() {
						// Subscriber가 어떠한 경우로든 Publisher에게 데이터를 그만보내라고 요청하는것
						// 그에 대비해 Publisher는 이 메소드안에 적절한 내용을 구현해두어야함
						// 일반적으로 flag를 두고 request쪽에서 데이터를 더 보내지 않도록 함
					}
				});
			}
		};
	}

	private static <T> Subscriber<T> logSub() {
		return new Subscriber<T>() {
			@Override
			public void onSubscribe(Subscription subscription) {
				System.out.println("onSubscribe");

				// 최초로 Publisher에게 데이터를 요청
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(T data) {
				System.out.println("onNext: " + data);
			}

			@Override
			public void onError(Throwable throwable) {
				System.out.println("onError: " + throwable);
			}

			@Override
			public void onComplete() {
				System.out.println("onComplete");
			}
		};
	}

}
