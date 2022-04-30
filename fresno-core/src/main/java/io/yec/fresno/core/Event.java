package io.yec.fresno.core;

import lombok.Data;

/**
 * io.yec.fresno.core.Event
 *
 * @author yecong
 * @date 2018/07/31
 */
@Data
public class Event<T> {
    private String eventType;
    private T obj;
}
