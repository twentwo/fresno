package io.yec.fresno.spring.support.annotation;


import java.lang.annotation.*;


/**
 * EventHandler annotation
 *
 * @author baijiu.yec
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface EventHandler {
    String value() default "";
}
