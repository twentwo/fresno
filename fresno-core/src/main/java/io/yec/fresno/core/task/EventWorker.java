package io.yec.fresno.core.task;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.yec.fresno.core.Event;
import io.yec.fresno.core.EventPublishThread;
import io.yec.fresno.core.queue.EventQueue;
import io.yec.fresno.core.task.handler.EventListener;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * io.yec.fresno.core.task.EventWorker
 *
 * @author yecong
 * @date 2018/07/31
 */
@Slf4j
@NoArgsConstructor
public class EventWorker {

    private int ringBufferSize = 1024;

    private int threadPoolSize = 8;

    private Map<EventQueue<?>, EventListener<?>> eventHandlerMap = Maps.newHashMap();

    private Map<String, EventQueue<?>> eventQueueMap = Maps.newHashMap();

    private Disruptor<Event<?>> disruptor;

    private List<EventPublishThread<?>> eventPublishThreads = Lists.newArrayList();

    private RingBuffer<Event<?>> ringBuffer;

    public EventWorker(int ringBufferSize, int threadPoolSize, Map<EventQueue<?>, EventListener<?>> eventQueueEventHandlerHashMap) {
        this.ringBufferSize = ringBufferSize;
        this.threadPoolSize = threadPoolSize;
        setEventHandlerMap(eventQueueEventHandlerHashMap);
    }

    private void setEventHandlerMap(Map<EventQueue<?>, EventListener<?>> eventHandlerMap) {
        if (Objects.isNull(eventHandlerMap)) {
            return;
        }
        this.eventHandlerMap = eventHandlerMap;
        if(!eventHandlerMap.isEmpty()) {
            this.eventQueueMap = Maps.newHashMap();
            for(Map.Entry<EventQueue<?>, EventListener<?>> entry : eventHandlerMap.entrySet()) {
                EventQueue<?> eventQueue = entry.getKey();
                this.eventQueueMap.put(eventQueue.getQueueName(), eventQueue);
            }
        }
    }

    /**
     * 初始化
     */
    public void init() {
        // 1. 创建Disruptor
        log.info("disruptor init...");
        if (eventHandlerMap.isEmpty()) {
            log.warn("disruptor init interrupt, eventHandler map is empty...");
            return;
        }
        disruptor = new Disruptor<>(
                // 事件工厂
                Event::new,
                // RingBuffer大小
                ringBufferSize,
                // 消费者线程池
                Executors.defaultThreadFactory(),
                // 支持多事件发布者
                ProducerType.MULTI,
                // 阻塞等待策略
                new BlockingWaitStrategy()
        );

        // 2. 获得ringBuffer
        ringBuffer = disruptor.getRingBuffer();

        // 3. 处理异常
        disruptor.setDefaultExceptionHandler(new ExceptionHandler<Event<?>>() {
            @Override
            public void handleEventException(Throwable throwable, long l, Event<?> event) {
                log.error("handleEventException", throwable);
            }
            @Override
            public void handleOnStartException(Throwable throwable) {
                log.error("handleOnStartException", throwable);
            }
            @Override
            public void handleOnShutdownException(Throwable throwable) {
                log.error("handleOnShutdownException", throwable);
            }
        });

        // 4. 创建工作者处理器
        WorkHandler<Event<?>> workHandler = event -> {
            String eventType = event.getEventType();
            // 4.1 根据事件类型取出EventQueue
            EventQueue eventQueue = eventQueueMap.get(eventType);
            // 4.2 根据EventQueue获取该队列的事件处理器EventHandler(XML中配置了关系)
            EventListener eventListener = eventHandlerMap.get(eventQueue);
            try {
                // 4.3 EventHandler处理事件
                eventListener.onEvent(eventQueue, event);
                // 处理成功
                eventQueue.success(event.getObj());
            } catch (Exception e) {
                log.error("event task handle exception, event: {}", event, e);
                eventQueue.fail(event.getObj());
            }
        };

        // 5.1 创建工作者处理器(数量为线程池大小)
        WorkHandler[] workHandlers = new WorkHandler[threadPoolSize];
        for (int i = 0; i < threadPoolSize; i++) {
            workHandlers[i] = workHandler;
        }

        // 5.2 告知 Disruptor 由工作者处理器处理
        disruptor.handleEventsWithWorkerPool(workHandlers);

        // 6. 启动disruptor
        disruptor.start();
        log.info("disruptor started...");

        // 7. 启动发布者线程(每个EventQueue一个, 可以优化为只有一个)
        for (Map.Entry<String, EventQueue<?>> eventQueueEntry : eventQueueMap.entrySet()) {
            String eventType = eventQueueEntry.getKey();
            EventQueue<?> eventQueue = eventQueueEntry.getValue();
            //每个类型的队列创建一个发布者
            EventPublishThread eventPublishThread = new EventPublishThread(eventType, eventQueue, ringBuffer);
            eventPublishThreads.add(eventPublishThread);
            eventPublishThread.start();
        }

    }

    public void stop() {
        // 1. 停止发布者线程
        for (EventPublishThread<?> eventPublishThread : eventPublishThreads) {
            eventPublishThread.shutdown();
        }
        // 2. 停止 Disruptor
        if (Objects.nonNull(disruptor)) {
            disruptor.shutdown();
        }
    }

}
