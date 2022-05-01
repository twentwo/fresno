package io.yec.fresno.core.task.handler;


import io.yec.fresno.core.Event;
import io.yec.fresno.core.queue.EventQueue;

/**
 * io.yec.fresno.core.task.handler.EventListener
 *
 * @author yecong
 * @date 2018/07/31
 */
public interface EventListener<T> {
    void onEvent(EventQueue<T> eventQueue, Event<T> event);
}
