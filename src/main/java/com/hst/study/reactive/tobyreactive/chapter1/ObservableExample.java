package com.hst.study.reactive.tobyreactive.chapter1;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author dlgusrb0808@gmail.com
 */
@SuppressWarnings("deprecation")
public class ObservableExample {

	static class IntObservable extends Observable implements Runnable {
		@Override
		public void run() {
			for (int i = 0; i < 10; i++) {
				// Observable에 변화가 생겼음을 알리는 메소드
				setChanged();
				// Observer들에게 변화를 알림 (값 포함)
				notifyObservers(i);
			}
		}
	}

	public static void main(String[] args) {
		Observer observer = new Observer() {
			@Override
			public void update(Observable observable, Object o) {
				System.out.println(String.format("%s %s", Thread.currentThread().getName(), o));
			}
		};

		IntObservable io = new IntObservable();
		io.addObserver(observer);	// Observable 등록

		ExecutorService es = Executors.newSingleThreadExecutor();
		es.execute(io);

		System.out.println(String.format("%s EXIT", Thread.currentThread().getName()));
		es.shutdown();
	}

}
