## Reactive Streams
### 표준 Reactive streams의 문제점
- 지금까지 봐온 코드는 전부 하나의 스레드에서 동작한다.
- 이 코드를 실전에서 활용하기엔 그닥 유용하지 않은 코드이다.
  - Publisher가 Blocking I/O를 사용하거나 데이터를 준비하는데 시간이 오래걸릴 경우 그걸 다 기다려야하기 때문이다.
  - 반대로 Publisher의 데이터 생성은 굉장히 빠른데, Subscriber의 데이터 처리가 늦을 경우도 마찬가지다. 
- Reactor에서는 `Scheduler`를 스레드 개념의 오퍼레이터를 활용해 이부분을 해결한다.
- 여기에서는 이 개념을 직접 구현한 코드를 보겠다.
- 아래 기본적인 Publisher와 Subscriber가 있다.

```java
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Publisher<Integer> pub = (sub) -> {
            sub.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    for (long i = 0; i < n; i++) {
                        Integer data = fetchData(); // 이 작업이 아주 오래걸리는 작업일 경우
                        sub.onNext(data);
                    }
                    sub.onComplete();
                }

                @Override
                public void cancel() {
                }
            });
        };

        pub.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                log.debug("onSubscribe");
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Integer i) {
                log.debug("onNext: {}", i);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("onError: {}", t);
            }

            @Override
            public void onComplete() {
                log.debug("onComplete");
            }
        });

        log.info("exit");
    }

}
```
- 이 코드를 실행하면 subscribe 후 모든 일이 main 스레드에서 실행되기 때문에 subscribe -> onSubscribe -> onNext* -> onComplete 후 "exit" 로그가 찍힌다.
- Publisher에서 데이터를 가져오는 `fetchData()` 메소드가 오래걸리기때문에 이 작업을 전부 다할때까지 기다려야한다.
- 보통은 이러한 작업은 백그라운드로 실행되어야 메인 프로그램 실행 흐름에 영향을 미치지 않고 처리할 수 있기 때문에 스레드를 활용한다.
- 여기 Publisher에 subscribe를 스레드에서 실행하는 오퍼레이터를 심은 Publisher를 만들어보면 아래와 같이 구현할 수 있다.

```java
Publisher<Integer> pub = normalPub();

Publisher<Integer> subOnPub = (sub) -> {
    // 별도 스레드를 생성하여 원래 처리할 publisher에게 subscriber 위임
    ExecutorService es = Executors.newSingleThreadExecutor();
    es.execute(() -> {
        pub.subscribe(sub);
    });
};

subOnPub.subscribe(subscriber());
```

- 이렇게 되면 메인 스레드와 별도의 스레드에서 기능이 실행되며 이후 프로그램 실행에는 영향을 주지 않게된다.
- Reactor에서는 이를 `subscribeOn` 메소드로 해결한다.
- 반대로 Subscriber에서 데이터를 처리하는게 오래걸리는 경우도 있다. 

```java
Publisher<Integer> pubOnSub = sub -> {
    pub.subscribe(new Subscriber<Integer>() {
        ExecutorService es = Executors.newSingleThreadExecutor();

        @Override
        public void onSubscribe(Subscription s) {
            sub.onSubscribe(s);
        }

        @Override
        public void onNext(Integer integer) {
            es.execute(() -> {
                sub.onNext(integer);
            });
        }

        @Override
        public void onError(Throwable t) {
            es.execute(() -> {
                sub.onError(t);
            });
        }

        @Override
        public void onComplete() {
            es.execute(() -> {
                sub.onComplete();
            });
        }
    });
};
```

- 위와 같이 Subscribe를 중개하면서 각 작업을 스레드에서 실행되게 wrapping하면 무거운 작업을 별도 스레드에서 처리할 수 있다.
- Reactor에서는 이를 `publishOn`이라는 메소드로 

### Reactor publishOn, subscribeOn
- 기본적으로 Reactor는 비동기를 강제하지는 않는다. (별도 스레드로 실행되는게 기본이 아니라는 뜻)
- 위에서 얘기한 내용 토대로 실제 Reactor에서 어떠한 식으로 구현했고 사용했는지 보자.
- `publishOn` 오퍼레이터는 데이터를 소비(consume)하는게 느릴 경우 사용한다.
- Subscriber쪽 코드가 별도 스레드에서 실행된다.

```java
Flux.range(1, 10)
    .publishOn(Schedulers.newSingle("pub"))
    .subscribe(data -> log.info("{}", data))
;
```

- `subscribeOn`은 데이터 생성 로직이 느릴 경우 사용한다.
- Publisher쪽 코드가 별도 스레드에서 실행된다.
```java
Flux.range(1, 10)
    .subscribeOn(Schedulers.newSingle("sub"))
    .subscribe(data -> log.info("{}", data))
;
```

- 혹은 아래와 같이 동시에 사용해도 문제없다.

```java
Flux.range(1, 10)
    .publishOn(Schedulers.newSingle("pub"))
    .subscribeOn(Schedulers.newSingle("sub"))
    .subscribe(data -> log.info("{}", data))
;
```

### 기본적으로 별도 스레드를 사용하는 Operators
- 위에서 말했든 Reactor는 일반적으로 비동기를 강제하지 않는다.
- 하지만 몇몇 Operators는 기본적으로 비동기로 동작하는 Operator들이 있다.
- 아래 예시코드를 보자

```java
Flux.intervel(Duration.ofMillis(200))   // 200ms 간격(interval)으로 데이터를 생성
    .take(5)                            // 최대 5개까지만 데이터를 수용함
    .subscribe(e -> log.info("{}", e)); // 데이터를 수신하면 로그를 출력함
```

- 이 코드를 pure reactive streams 로 구현해보면 아래와 같이 구현할 수 있다.

```java
// 일정 주기로 데이터를 발행하는 퍼블리셔
Publisher<Integer> pub = (sub) -> {
    sub.onSubscribe(new Subscription() {
        int value = 0;
        boolean cancelled = false;

        @Override
        public void request(long max) {
            // 아래 Executor는 일정 주기로 동작하는 작업를 구현할 때 용이함
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

            // 초기 딜레이 0, 작업간 딜레이 200ms 간격으로 스케쥴링
            exec.scheduleAtFixedRate(() -> {
                // 작업이 종료되었으면 (cancel이 호출된 경우) 스케쥴링 종료
                if (cancelled) {
                    exec.shutdown();                    
                    return;
                }
                // 데이터 발생
                sub.onNext(value++);
            }, 0, 200, TimeUnit.MILLISECONDS);
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }
    });
};

// 일정 갯수가 발행되면 발행을 중단시키는 퍼블리셔
// 내부적으로 기존 subscriber를 사용하되 일정 갯수가 되면 cancel을 날리는 Subscriber를 새로 생성
Publiser<Integer> takePub = (sub) -> {
    pub.subscribe(new Subscriber() {
        int count = 0;
        Subscription s;

        @Override
        public void onSubscribe(Subscription s) {
            sub.onSubscribe(s);
            this.s = s;
        }

        @Override
        public void onNext(Integer integer) {            
            // 기존 Subscriber로 처리 위임 후 카운트 증가
            sub.onNext(integer);
            count++;

            // 정해진 횟수를 넘어서면 cancel 호출
            if (count >= 5) {
                s.cancel();
            }
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
};

Subscriber<Integer> logSub = new Subscriber<>() {
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
};

takePub.subscribe(logSub);
```