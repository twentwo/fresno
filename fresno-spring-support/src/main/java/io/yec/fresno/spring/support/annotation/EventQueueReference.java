package io.yec.fresno.spring.support.annotation;


import io.yec.fresno.core.task.handler.EventListener;

import java.lang.annotation.*;


/**
 * EventQueue annotation
 *
 * @author baijiu.yec
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface EventQueueReference {
    /**
     * belong to a EventHandler
     * @return
     */
    Class<? extends EventListener> belongTo() default EventListener.class;
}
