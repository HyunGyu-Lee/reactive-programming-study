package com.hst.study.reactive.tobyreactive.chapter6;

import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author dlgusrb0808@gmail.com
 */
public class Ex {

	public static void main(String[] args) {
		String uri = "http://localhost:8081/service?req={req}";
		String errorUri = "http://localhost:8081/service?req={req}";

		DeferredResult<String> dr = new DeferredResult<>();

		AsyncRestTemplate rt = new AsyncRestTemplate();
		Completion
				.from(rt.getForEntity(errorUri, String.class, "hi"))
				.andAccept(s -> dr.setResult(s.getBody()));

		while (true) {
			if (dr.hasResult()) {
				System.out.println(dr.getResult());
				break;
			}
		}
	}

	static class Completion {
		Completion next;
		Consumer<ResponseEntity<String>> con;

		Completion() {}

		Completion(Consumer<ResponseEntity<String>> con) {
			this.con = con;
		}

		public void andAccept(Consumer<ResponseEntity<String>> con) {
			Completion c = new Completion(con);
			this.next = c;
		}

		private void complete(ResponseEntity<String> r) {
			if (next != null) {
				next.run(r);
			}
		}

		private void run(ResponseEntity<String> r) {
			if (con != null) {
				con.accept(r);
			}
		}

		private void error(Throwable e) {

		}

		public static Completion from(ListenableFuture<ResponseEntity<String>> lf) {
			Completion c = new Completion();
			lf.addCallback((res) -> {
				c.complete(res);
			}, (e) -> {
				c.error(e);
			});
			return c;
		}

	}

}
