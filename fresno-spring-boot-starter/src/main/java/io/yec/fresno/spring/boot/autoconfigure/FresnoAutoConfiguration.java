package io.yec.fresno.spring.boot.autoconfigure;

import com.google.common.collect.Maps;
import com.lmax.disruptor.dsl.Disruptor;
import io.yec.fresno.core.queue.EventQueue;
import io.yec.fresno.core.task.EventHandlerBean;
import io.yec.fresno.core.task.EventWorker;
import io.yec.fresno.core.task.handler.EventListener;
import io.yec.fresno.core.task.proxy.OnEventProxyAdvice;
import io.yec.fresno.core.task.proxy.OnEventProxyCreator;
import io.yec.fresno.spring.boot.autoconfigure.properties.FresnoConfigProperties;
import io.yec.fresno.spring.boot.autoconfigure.properties.FresnoEventConfigProperties;
import io.yec.fresno.spring.support.config.EventHandlerAnnotationBeanPostProcessor;
import io.yec.fresno.spring.support.config.EventQueueReferenceAnnotationBeanPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.yec.fresno.spring.boot.autoconfigure.utils.FresnoUtils.*;
import static io.yec.fresno.spring.support.config.EventHandlerAnnotationBeanPostProcessor.FRESNO_REDIS_TEMPLATE_BEAN_NAME;


/**
 * FresnoAutoConfiguration
 *
 * @author baijiu.yec
 * @since 2022/04/28
 */
@Slf4j
@ConditionalOnProperty(prefix = FRESNO_PREFIX, name = "enabled", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(value = {
        FresnoConfigProperties.class,
        FresnoEventConfigProperties.class
})
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class FresnoAutoConfiguration {

    @ConditionalOnMissingBean(name = FRESNO_REDIS_TEMPLATE_BEAN_NAME)
    @ConditionalOnBean(RedisConnectionFactory.class)
    @Bean(name = FRESNO_REDIS_TEMPLATE_BEAN_NAME)
    public RedisTemplate fresnoRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return redisTemplate;
    }

    /**
     * The bean is used to scan the packages of Dubbo Service classes
     *
     * @param environment {@link Environment} instance
     * @return non-null {@link Set}
     * @since 0.0.1
     */
    @ConditionalOnProperty(prefix = FRESNO_SCAN_PREFIX, name = BASE_PACKAGES_PROPERTY_NAME)
    @ConditionalOnMissingBean(name = BASE_PACKAGES_BEAN_NAME)
    @Bean(name = BASE_PACKAGES_BEAN_NAME)
    public Set<String> fresnoBasePackages(Environment environment) {
        FresnoConfigProperties fresnoConfigProperties = Binder.get(environment)
                .bind(FRESNO_PREFIX, FresnoConfigProperties.class)
                .orElse(null);
        return Objects.isNull(fresnoConfigProperties) ? null : fresnoConfigProperties.getScan().getBasePackages();
    }

    /**
     * Creates {@link EventHandlerAnnotationBeanPostProcessor} Bean
     *
     * @param packagesToScan the packages to scan
     * @return {@link EventHandlerAnnotationBeanPostProcessor}
     */
    @ConditionalOnBean(name = BASE_PACKAGES_BEAN_NAME)
    @Bean
    public EventHandlerAnnotationBeanPostProcessor eventHandlerAnnotationBeanPostProcessor(@Qualifier(BASE_PACKAGES_BEAN_NAME) Set<String> packagesToScan) {
        return new EventHandlerAnnotationBeanPostProcessor(packagesToScan);
    }

    /**
     * Creates {@link EventQueueReferenceAnnotationBeanPostProcessor} Bean if Absent
     *
     * @return {@link EventQueueReferenceAnnotationBeanPostProcessor}
     */
    @ConditionalOnMissingBean(name = EventQueueReferenceAnnotationBeanPostProcessor.BEAN_NAME)
    @ConditionalOnBean(EventHandlerAnnotationBeanPostProcessor.class)
    @Bean(name = EventQueueReferenceAnnotationBeanPostProcessor.BEAN_NAME)
    public EventQueueReferenceAnnotationBeanPostProcessor eventQueueReferenceAnnotationBeanPostProcessor() {
        return new EventQueueReferenceAnnotationBeanPostProcessor();
    }

    @ConditionalOnMissingBean(name = "onEventProxyCreator")
    @ConditionalOnBean(EventHandlerAnnotationBeanPostProcessor.class)
    @Bean
    public OnEventProxyCreator onEventProxyCreator() {
        return new OnEventProxyCreator(new OnEventProxyAdvice());
    }

    @ConditionalOnMissingBean(EventWorker.class)
    @ConditionalOnClass(Disruptor.class)
    @ConditionalOnBean(name = EventQueueReferenceAnnotationBeanPostProcessor.BEAN_NAME)
    @Bean(initMethod = "init", destroyMethod = "stop")
    public EventWorker eventWorker(List<EventHandlerBean> eventHandlerBeans, FresnoEventConfigProperties eventConfigProperties) {
        Map<EventQueue<?>, EventListener<?>> eventQueueEventHandlerHashMap = Maps.newHashMap();
        eventHandlerBeans.stream().forEach(eventHandlerBean -> {
            eventQueueEventHandlerHashMap.put(eventHandlerBean.getEventQueue(), eventHandlerBean.getEventListener());
        });
        EventWorker eventWorker = new EventWorker(eventConfigProperties.getWorker().getRingBufferSize(), eventConfigProperties.getWorker().getThreadPoolSize(), eventQueueEventHandlerHashMap);
        return eventWorker;
    }

}
