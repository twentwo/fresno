package io.yec.fresno.spring.support.config;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Fresno {@link io.yec.fresno.core.queue.EventQueue} Bean Builder
 *
 * @see io.yec.fresno.core.queue.EventQueue
 * @since 0.0.1
 */
public class EventQueueBeanNameBuilder {

    private static final String SEPARATOR = ":";

    // Required
    private final String eventHandlerAnnotatedClassName;

    private final Environment environment;

    private EventQueueBeanNameBuilder(Class<?> eventHandlerAnnotatedClass, Environment environment) {
        this(eventHandlerAnnotatedClass.getName(), environment);
    }

    private EventQueueBeanNameBuilder(String eventHandlerAnnotatedClassName, Environment environment) {
        this.eventHandlerAnnotatedClassName = eventHandlerAnnotatedClassName;
        this.environment = environment;
    }

    public static EventQueueBeanNameBuilder create(Class<?> eventHandlerAnnotatedClass, Environment environment) {
        return new EventQueueBeanNameBuilder(eventHandlerAnnotatedClass, environment);
    }

    private static void append(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(SEPARATOR).append(value);
        }
    }

    public String build() {
        StringBuilder beanNameBuilder = new StringBuilder("EventQueueBean");
        // Required
        append(beanNameBuilder, eventHandlerAnnotatedClassName);
        // Build and remove last ":"
        String rawBeanName = beanNameBuilder.toString();
        // Resolve placeholders
        return environment.resolvePlaceholders(rawBeanName);
    }

}
