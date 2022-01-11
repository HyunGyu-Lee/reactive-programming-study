package com.hst.study.reactive.tobyreactive.chapter5;

import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Copyright 2022 NHN Corp. All rights Reserved.
 * NHN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @author hyungyu.lee@nhn.com
 * @date 2022-01-11
 */
public class Example {

	public static void main(String[] args) {
		String uri = "http://localhost:8081/service?req={req}";
		String errorUri = "http://localhost:8081/error-service?req={req}";
		AsyncRestTemplate rt = new AsyncRestTemplate();

		Completion
				.from(rt.getForEntity(uri, String.class, "hi"))
				.onError(s -> System.out.println("asdkmlasdkmlasdml    " + s.getMessage()))
				.andThen(s -> rt.getForEntity(errorUri, String.class, s.getBody()))
				.accept(s -> System.out.println(s.getBody()))
		;
	}

	public static class AcceptCompletion extends Completion {
		private Consumer<ResponseEntity<String>> consumer;

		public AcceptCompletion(Consumer<ResponseEntity<String>> consumer) {
			this.consumer = consumer;
		}

		@Override
		protected void run(ResponseEntity<String> res) {
			consumer.accept(res);
		}
	}

	public static class AndThenCompletion extends Completion {
		private Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> andThen;

		public AndThenCompletion(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> andThen) {
			this.andThen = andThen;
		}

		@Override
		protected void run(ResponseEntity<String> res) {
			ListenableFuture<ResponseEntity<String>> lf = andThen.apply(res);
			lf.addCallback(this::complete, this::error);
		}
	}

	public static class ErrorCompletion extends Completion {
		private Consumer<Throwable> consumer;

		public ErrorCompletion(Consumer<Throwable> consumer) {
			this.consumer = consumer;
		}

		@Override
		void run(ResponseEntity<String> res) {
			if (next != null) {
				next.run(res);
			}
		}

		@Override
		protected void error(Throwable e) {
			System.out.println("??");
			consumer.accept(e);
		}
	}

	public static abstract class Completion {
		protected Completion next;

		protected void complete(ResponseEntity<String> res) {
			if (next != null) {
				this.next.run(res);
			}
		}

		protected void error(Throwable e) {
			System.out.println("CALL >>> " + getClass().getName());
			if (next != null) {
				next.error(e);
			}
		}

		abstract void run(ResponseEntity<String> res);

		public Completion onError(Consumer<Throwable> con) {
			Completion c = new ErrorCompletion(con);
			this.next = c;
			return c;
		}

		public Completion andThen(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn) {
			Completion c = new AndThenCompletion(fn);
			this.next = c;
			return c;
		}

		public void accept(Consumer<ResponseEntity<String>> con) {
			Completion c = new AcceptCompletion(con);
			this.next = c;
		}

		public static Completion from(ListenableFuture<ResponseEntity<String>> s) {
			Completion c = new Completion() {
				@Override
				void run(ResponseEntity<String> res) {
				}
			};
			s.addCallback(c::complete, c::error);
			return c;
		}

	}

}
