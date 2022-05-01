package io.yec.fresno.example.service.impl;

import io.yec.fresno.core.queue.EventQueue;
import io.yec.fresno.example.dto.Order;
import io.yec.fresno.example.dto.User;
import io.yec.fresno.example.listener.OrderEventListener;
import io.yec.fresno.example.listener.UserEventListener;
import io.yec.fresno.example.service.FooService;
import io.yec.fresno.spring.support.annotation.EventQueueReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FooServiceImpl
 *
 * @author baijiu.yec
 * @since 2022/04/19
 */
@Slf4j
@Service
public class FooServiceImpl implements FooService {

    @EventQueueReference(belongTo = OrderEventListener.class)
    private EventQueue<Order> orderEventQueue;

    @EventQueueReference(belongTo = UserEventListener.class)
    private EventQueue<User> userEventQueue;

    @Override
    public void putUser(User user) {
        userEventQueue.enqueueToBack(user);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void sayHello(Object eventBody) {
        System.out.printf("Hello^^^^^^^^^^%s\n", eventBody);
    }

    @Override
    public void enQueueOrder(Order order) {
        log.info("enQueueOrder :: {}", order);
        orderEventQueue.enqueueToBack(order);
    }

    @Override
    public void getOrder(Order order) {
        System.out.printf("Order^^^^^^^^^^%s\n", order);
    }
}
