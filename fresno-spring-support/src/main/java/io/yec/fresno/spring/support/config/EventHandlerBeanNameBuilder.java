package io.yec.fresno.spring.support.config;

import io.yec.fresno.spring.support.annotation.EventHandler;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Fresno {@link EventHandler @EventHandler} Bean Builder
 *
 * @see EventHandler
 * @see io.yec.fresno.core.task.EventHandlerBean
 * @since 0.0.1
 */
public class EventHandlerBeanNameBuilder {

    private static final String SEPARATOR = ":";

    // Required
    private final String eventHandlerAnnotatedClassName;

    private final Environment environment;

    private EventHandlerBeanNameBuilder(Class<?> interfaceClass, Environment environment) {
        this(interfaceClass.getName(), environment);
    }

    private EventHandlerBeanNameBuilder(String eventHandlerAnnotatedClassName, Environment environment) {
        this.eventHandlerAnnotatedClassName = eventHandlerAnnotatedClassName;
        this.environment = environment;
    }

    public static EventHandlerBeanNameBuilder create(Class<?> eventHandlerAnnotatedClassName, Environment environment) {
        return new EventHandlerBeanNameBuilder(eventHandlerAnnotatedClassName, environment);
    }

    private static void append(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(SEPARATOR).append(value);
        }
    }

    public String build() {
        StringBuilder beanNameBuilder = new StringBuilder("EventHandlerBean");
        // Required
        append(beanNameBuilder, eventHandlerAnnotatedClassName);
        // Build and remove last ":"
        String rawBeanName = beanNameBuilder.toString();
        // Resolve placeholders
        return environment.resolvePlaceholders(rawBeanName);
    }
}
