package com.hst.study.reactive.tobyreactive.chapter2;

import reactor.core.publisher.Flux;

/**
 * @author dlgusrb0808@gmail.com
 */
public class ReactorExample {

	public static void main(String[] args) {
		Flux.<Integer>create(e -> {
			e.next(1);
			e.next(2);
			e.next(3);
			e.complete();
		})
		.log()
		.map(e -> e * 10)
		.log()
		.subscribe(s -> System.out.println(s));
	}

}
