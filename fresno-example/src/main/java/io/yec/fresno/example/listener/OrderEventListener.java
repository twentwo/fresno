package io.yec.fresno.example.listener;

import io.yec.fresno.core.Event;
import io.yec.fresno.core.queue.EventQueue;
import io.yec.fresno.core.task.handler.EventListener;
import io.yec.fresno.example.dto.Order;
import io.yec.fresno.example.service.FooService;
import io.yec.fresno.spring.support.annotation.EventHandler;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;


/**
 * io.yec.fresno.example.listener.FundsEventListener
 *
 * @author yecong
 * @date 2018/08/01
 */
@Slf4j
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
