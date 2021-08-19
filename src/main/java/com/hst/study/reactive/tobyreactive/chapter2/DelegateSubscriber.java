package com.hst.study.reactive.tobyreactive.chapter2;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

/**
 * @author dlgusrb0808@gmail.com
 */
public class DelegateSubscriber<T, R> implements Subscriber<T> {
	private Subscriber subscriber;

	public DelegateSubscriber(Subscriber<? super R> subscriber) {
		this.subscriber = subscriber;
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		subscriber.onSubscribe(subscription);
	}

	@Override
	public void onNext(T data) {
		subscriber.onNext(data);
	}

	@Override
	public void onError(Throwable throwable) {
		subscriber.onError(throwable);
	}

	@Override
	public void onComplete() {
		subscriber.onComplete();
	}
}
