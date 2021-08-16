package com.hst.study.reactive.tobyreactive.chapter1;

import java.util.Iterator;

/**
 * @author dlgusrb0808@gmail.com
 */
public class IterableExample {

	static class IntegerIterable implements Iterable<Integer> {
		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<>() {
				private final int max = 10;
				private int i = 0;

				@Override
				public boolean hasNext() {
					return i < max;
				}

				@Override
				public Integer next() {
					return ++i;
				}
			};
		}
	}

	public static void main(String[] args) {
		IntegerIterable iter = new IntegerIterable();
		for (int i : iter) {
			System.out.println(i);
		}
	}

}
