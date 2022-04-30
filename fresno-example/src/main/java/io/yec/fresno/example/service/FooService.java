package io.yec.fresno.example.service;

import io.yec.fresno.example.dto.Order;
import io.yec.fresno.example.dto.User;

/**
 * FooService
 *
 * @author baijiu.yec
 * @since 2022/04/19
 */
public interface FooService {
    void putUser(User user);
    void sayHello(Object eventBody);
    void enQueueOrder(Order order);
    void getOrder(Order order);
}
