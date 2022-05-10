package io.yec.fresno.spring.support.config;

import com.alibaba.spring.beans.factory.annotation.AbstractAnnotationBeanPostProcessor;
import io.yec.fresno.core.task.handler.EventListener;
import io.yec.fresno.spring.support.annotation.EventQueueReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAttributes;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static com.alibaba.spring.util.AnnotationUtils.getAttributes;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that EventQueue {@link EventQueueReference} annotated fields
 *
 * @since 0.0.1
 */
@Slf4j
public class EventQueueReferenceAnnotationBeanPostProcessor extends AbstractAnnotationBeanPostProcessor implements
        ApplicationContextAware {

    /**
     * The bean name of {@link EventQueueReferenceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "eventQueueReferenceAnnotationBeanPostProcessor";

    private ApplicationContext applicationContext;

    /**
     * To support the legacy annotation that is @io.yec.fresno.spring.annotationEventQueueReference since 0.0.1
     */
    public EventQueueReferenceAnnotationBeanPostProcessor() {
        super(EventQueueReference.class);
    }

    @Override
    protected Object doGetInjectedBean(AnnotationAttributes attributes, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {
        /**
         * The name of bean that is declared by {@link Reference @Reference} annotation injection
         */
        String referenceBeanName = getEventQueueReferenceBeanName(attributes);

        return getBeanFactory().getBean(referenceBeanName);
    }

    /**
     * Get the bean name of {@link io.yec.fresno.core.queue.EventQueue} if {@link EventQueueReference#belongTo()} () belongTo attribute} is present.
     *
     * @param attributes     the {@link AnnotationAttributes attributes} of {@link EventQueueReference @EventQueueReference}
     * @return non-null
     * @since 0.0.1
     */
    private String getEventQueueReferenceBeanName(AnnotationAttributes attributes) throws IllegalAccessException {
        Class<?> belongToEventListenerClass = getAttribute(attributes, "belongTo");
        if (belongToEventListenerClass.isInterface() || !EventListener.class.isAssignableFrom(belongToEventListenerClass)) {
            throw new IllegalAccessException("illegal @EventQueueReference#belongTo()");
        }
        return EventQueueBeanNameBuilder.create(belongToEventListenerClass, applicationContext.getEnvironment()).build();
    }

    @SneakyThrows
    @Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes attributes, Object bean, String beanName,
                                                 Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        return getEventQueueReferenceBeanName(attributes) +
                "#source=" + (injectedElement.getMember()) +
                "#attributes=" + getAttributes(attributes, getEnvironment());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
