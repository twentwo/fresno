package io.yec.fresno.spring.support.config;

import io.yec.fresno.spring.support.annotation.EventHandler;
import io.yec.fresno.spring.support.annotation.EventHandlerComponentScan;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.alibaba.spring.util.BeanRegistrar.registerInfrastructureBean;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * Fresno {@link EventHandlerComponentScan} Bean Registrar
 *
 * @see EventHandler
 * @see EventHandlerComponentScan
 * @see ImportBeanDefinitionRegistrar
 * @see EventHandlerAnnotationBeanPostProcessor
 * @since 0.0.1
 */
public class EventHandlerComponentScanRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);
        registerEventHandlerAnnotationBeanPostProcessor(packagesToScan, registry);

        registerEventQueueReferenceAnnotationBeanPostProcessor(registry);

    }

    /**
     * Registers {@link EventHandlerAnnotationBeanPostProcessor}
     *
     * @param packagesToScan packages to scan without resolving placeholders
     * @param registry       {@link BeanDefinitionRegistry}
     * @since 0.0.1
     */
    private void registerEventHandlerAnnotationBeanPostProcessor(Set<String> packagesToScan, BeanDefinitionRegistry registry) {

        BeanDefinitionBuilder builder = rootBeanDefinition(EventHandlerAnnotationBeanPostProcessor.class);
        builder.addConstructorArgValue(packagesToScan);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);

    }

    /**
     * Registers {@link EventQueueReferenceAnnotationBeanPostProcessor} into {@link BeanFactory}
     *
     * @param registry {@link BeanDefinitionRegistry}
     */
    private void registerEventQueueReferenceAnnotationBeanPostProcessor(BeanDefinitionRegistry registry) {

        // Register @EventQueueReference Annotation Bean Processor
        registerInfrastructureBean(registry,
                EventQueueReferenceAnnotationBeanPostProcessor.BEAN_NAME, EventQueueReferenceAnnotationBeanPostProcessor.class);

    }

    private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(EventHandlerComponentScan.class.getName()));
        String[] basePackages = attributes.getStringArray("basePackages");
        Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
        String[] value = attributes.getStringArray("value");
        // Appends value array attributes
        Set<String> packagesToScan = new LinkedHashSet<String>(Arrays.asList(value));
        packagesToScan.addAll(Arrays.asList(basePackages));
        for (Class<?> basePackageClass : basePackageClasses) {
            packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
        }
        if (packagesToScan.isEmpty()) {
            return Collections.singleton(ClassUtils.getPackageName(metadata.getClassName()));
        }
        return packagesToScan;
    }

}
