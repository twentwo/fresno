package io.yec.fresno.core.task;

import io.yec.fresno.core.queue.EventQueue;
import io.yec.fresno.core.task.handler.EventListener;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EventHandlerBean
 *
 * @author baijiu.yec
 * @since 2022/04/18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventHandlerBean {

    private EventQueue eventQueue;
    private EventListener eventListener;

}
