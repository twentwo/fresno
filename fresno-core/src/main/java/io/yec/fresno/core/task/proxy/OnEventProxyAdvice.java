package io.yec.fresno.core.task.proxy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.yec.fresno.core.Event;
import io.yec.fresno.core.queue.EventQueue;
import io.yec.fresno.core.task.handler.EventListener;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * {@link EventListener#onEvent(EventQueue, Event)} proxy advice
 *
 * @author baijiu.yec
 * @since 2022/05/01
 */
@Slf4j
public class OnEventProxyAdvice implements MethodInterceptor {

    private static ObjectMapper objectMapper = new ObjectMapper()
            // support Java 8 DateTime
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method targetMethod = methodInvocation.getMethod();
        Object[] args = methodInvocation.getArguments();
        Event event = (Event) args[1];
        Object obj = event.getObj();
        if (obj instanceof HashMap) {
            Class<?> eventObjType  = (Class<?>) ResolvableType.forMethodParameter(targetMethod, 1).getGeneric(0).getType();
            Object eventObj = objectMapper.convertValue(obj, eventObjType);
            event.setObj(eventObj);
        }
        return methodInvocation.proceed();
    }

}
