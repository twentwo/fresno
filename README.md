# fresno

A Disruptor + Redis Queue

draw inspiration from https://github.com/zhangkaitao

## Architecture

![a-disrupt-redis-queue](https://twentwo.github.io/baijiu.yec/blog/2022/a-disrupt-redis-queue/Disrupt%2BRedis%20Queue.png)



## Usage

spring-boot application support

### Maven

maven dependency import

```xml
<dependency>
  <groupId>io.yec</groupId>
  <artifactId>fresno-spring-boot-starter</artifactId>
  <version>${fresno.version}</version>
</dependency>
```

### EventHandler

define EventHandler

```java
@EventHandler
public class OrderEventListener implements EventListener<Order> {

    @Resource
    private FooService fooService;

    @Override
    public void onEvent(EventQueue<Order> eventQueue, Event<Order> event) {
        try {
            fooService.getOrder(event.getObj());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
```

### EventQueueReference

reference EventQueue

```java
@Service
public class FooServiceImpl implements FooService {

    @EventQueueReference(belongTo = OrderEventListener.class)
    private EventQueue<Order> orderEventQueue;

    @Override
    public void enQueueOrder(Order order) {
        log.info("enQueueOrder :: {}", order);
        orderEventQueue.enqueueToBack(order);
    }
}
```

