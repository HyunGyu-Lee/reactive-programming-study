## Reactive Streams

### ExecutorService
- 스레드를 쉽게 다룰수 있게 도와주는 오브젝트
- execute(Runnable runnable) : 리턴값이 없는 비동기 작업을 수행할 때 사용
- submit(Callable callable) : 리턴값이 있는 비동기 작업을 수행할 때 사용
  - Callable<T> 인터페이스는 리턴값이 있고, 예와를 던지는 T call() 메소드로 이루어져있음
- execute는 반환형이 없고(void) submit은 반환형이 `Future`이다.

### Future 인터페이스
기본적으로 비동기라 함은 어떤 작업을 별도의 스레드를 통해 수행하는 것을 의미한다.
`Future` 인터페이스는 어떤 비동기 작업의 **결과**를 나타내는 인터페이스이다.

- get() 메소드를 통해 비동기 작업의 리턴값을 가져올 수 있다. 단, 비동기 작업이 끝날때 까지 blocking됨
- isDone() 메소드를 톧해 비동기 작업이 끝났는지 확인할 수 있다. blocking 없이 바로 확인 가능

결과값을 가져오는 get() 메소드가 블로킹이라는 점에서 크게 아쉬운 부분이 있다.

### FutureTask 클래스
- `FutureTask`는 `Future`인터페이스를 구현한 클래스 중의 하나로 비동기로 실행할 작업까지 같이 가지고 있는 오브젝트이다.
- ExecutorService를 통해 실행될 수 있으면서 동시에 결과값을 가져올 수도 있다.
- `Runnable` 인터페이스또한 구현하고 있어 execute를 통해 실행가능하다.
- `done()` 훅 메소드가 제공되어 비동기 작업이 끝난 시점에 실행된다.
- 이 두가지를 종합해보면, FutureTask를 통한다면 콜백으로 사용할 수 있는 메커니즘이 구현되어있다는것을 알 수 있다.

```java
ExecutorService es = Executors.newCachedThreadPool();
FutureTask<String> ft = new FutureTask<>(() -> {
  Thread.sleep(2000);
  logger.info("In Async");
  return "Hello";
}) {
    @Override
    protected void done() {
        try {
            System.out.println("Async Task result is " + this.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }    	
};
es.submit(ft);
```

- 아래는 FutureTask를 확장하여 callback 사용가능한 클래스로 만들어볼 수 있다.

```java
import java.util.Objects;

interface SuccessCallback {
  void onSuccess(String result);
}

interface ExceptionCallback {
  void onError(Throwable t);
}

public static class CallableFutureTask extends FutureTask<String> {
  SuccessCallback callback;
  ExceptionCallback exceptionCallback;

  public CallableFutureTask(Callable<String> callable, SuccessCallback callback, ExceptionCallback exceptionCallback) {
    super(callable);
    this.callback = Objects.requireNonNull(callback);
    this.exceptionCallback = Objects.requireNonNull(exceptionCallback);
  }

  @Override
  protected void done() {
    try {
      callback.onSuccess(get());
    } catch (InterruptedException e) {  // 이 예외는 보통 인터럽트 발생을 전파 해주는 경우로 충분
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {    // 이 예외는 비동기 작업 수행중에 오류가 발생한 것이기 때문에 반드시 처리해주어야한다.
      exceptionCallback.onError(e);
    }
  }
}
```

### Spring 에서의 비동기 기술
스프링에서는 아주 간단하게 비동기 기술을 적용할 수 있다.
`@EnableAsync`를 통해 비동기 기능을 활성화하고, 비동기로 수행해야할 컴포넌트 메소드에 `@Async` 어노테이션을
설정해주면 된다.

단, 결과를 리턴하는 경우엔 `Future`타입으로 반환되어야 결과를 제대로 받아 쓸 수 있다.
`ListenableFuture<T>` 를 반환하면 호출 측에서 성공/실패 콜백을 지정할 수 있다.

`@Async` 어노테이션에 bean이름을 주어 어떤 스레드 풀로 돌릴건지 지정할 수 있다.

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@SpringBootApplication
@EnableAsync
public class App {

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }

  @Service
  public static class MyService {

    @Async
    public Future<String> greeting() {
      return new AsyncResult<>("greeting");
    }

	@Async
    public ListenableFuture<String> greeting2() {
      return new AsyncResult<>("greeting2");
    }

  }

}
```

### Spring 비동기 스레드풀 정책 설정
스프링 비동기 기술은 기본적으로 ExecutorService를 활용해 제공되며, 관련하여 설정이 가능하다.
아래 예시는 스레드 풀 사이즈 정책, 스레드 네이밍을 설정하는 예시 코드이다.
- corePoolSize: 기본적으로 동시에 실행될 수 있는 작업의 갯수라고 생각하면 된다.
만약 1로 설정되어있으면 싱글 스레드로 동작하는 것과 똑같다.
- queueCapacity: 스레드풀엔 corePoolSize가 모두 사용중일 때 그 이후에 들어온 요청들이 대기하는 큐가 있다. 
이 설정은 해당 큐의 크기를 설정하는 것이다. 기본값은 Integer.MAX_VALUE여서 설정안할 시 maxPoolSize 수치는 사실 의미가 없다.
- maxPoolSize: 만약 대기 큐가 가득차게 되면, 스레드풀 사이즈를 corePoolSize(초기값)에서 이 값으로 증가시킨다.

```java
@Bean
ThreadPoolTaskExecutor es() {
    ThreadPoolTaskExecutor es = new ThreadPoolTaskExecutor();
    es.setCorePoolSize(10);	// 기본적으로 초기에 만들어둘 스레드 개수
    es.setMaxPoolSize(100);	// 큐까지 가득차면 맥스 풀 사이즈만큼 늘
    es.setQueueCapacity(200);	// 코어 풀 사이즈만큼 모두 사용중인 상태로 이후에 들어온 요청은 큐에서 대기
    es.setThreadNamePrefix("mythread-");
    es.initialize();
    return es;
}
```

### 웹 애플리케이션에서의 동기 / 비동기
기본적으로 웹은 Request -> Logic -> Response (html, json, etc..) 의 구조로 이뤄진다.
하나의 요청이 들어오면 1개의 스레드가 할당되어 위 흐름대로 진행되고 스레드풀 이상의 요청은 큐에서 대기한다.
만약 로직에 시간이 오래걸리는 작업이 있을경우 (DB, 외부 API 호출 등) 특정 스레드가 블록킹된 상태로 기다리고 있는채로
다른 요청을 진행 못시키는 경우가 생긴다. 
Logic에서 오래걸리는 작업을 비동기(별도 스레드)로 실행하더라도 서블릿 스레드 자체는 해당 요청을 물고있기때문에
스레드가 빠르게 고갈된다.
때문에 실제로 무거운 로직이 별도로 실행되고, 서블릿 스레드는 다른 요청을 처리해야할 필요성이 있다.

웹에서의 비동기 기술은 이런 문제점을 개선하기 위해 등장하게 됐다. 

### Servlet 3.0의 등장과 비동기
Servlet 3.0에서는 기존 동기 방식과 달리 이런 부분을 어느 정도 해결한 구조였다.
- HTTP 커넥션은 이미 논블로킹 (톰캣 NIO, Jetty NIO 등). 서블릿의 동작과는 별개
- 서블릿 Request read, Response write는 블로킹
- 서블릿 스레드 내부에 비동기 작업(별도 스레드)이 시작되면 해당 서블릿 스레드 즉시 반납
- 비동기 작업이 완료되면 서블릿 스레드 재할당
- Servlet 3.1에선 서블릿 Request read, Response write까지 비동기로 진행

### Spring Web 레이어에서의 비동기 사용
위 예시들은 코어 / 비즈니스 레벨에서의 비동기 기술에 대한 예시였다.

