package io.yec.fresno.core;

import com.lmax.disruptor.RingBuffer;
import io.yec.fresno.core.queue.EventQueue;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * io.yec.fresno.core.EventPublishThread
 *
 * @author yecong
 * @date 2018/07/31
 */
@Slf4j
@ToString(exclude = {"ringBuffer"})
public class EventPublishThread<T> extends Thread {

    private EventQueue<T> eventQueue;
    private RingBuffer<Event<T>> ringBuffer;
    private String eventType;
    private boolean running;

    public EventPublishThread(String eventType, EventQueue<T> eventQueue, RingBuffer<Event<T>> ringBuffer) {
        this.eventQueue = eventQueue;
        this.ringBuffer = ringBuffer;
        this.eventType = eventType;
        this.running = true;
    }

    @Override
    public void run() {
        // 当调用shutdown方法时, 设置为false即可
        log.info("start event publish thread -> {}", this);
        while (running) {
            try {
                // 从EventQueue获取下一个任务
                T next = eventQueue.next();
                if (next != null) {
                    log.debug("fetch event task -> {}", next);
                    // 发布到RingBuffer
                    ringBuffer.publishEvent((event, sequence) -> {
                        event.setEventType(eventType);
                        event.setObj(next);
                    });
                }
            } catch (Exception ex) {

            }
        }
    }

    public void shutdown() {
        running = false;
    }

}
