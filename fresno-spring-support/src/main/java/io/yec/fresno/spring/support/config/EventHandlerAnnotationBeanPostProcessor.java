package io.yec.fresno.spring.support.config;

import io.yec.fresno.core.queue.EventQueue;
import io.yec.fresno.core.task.EventHandlerBean;
import io.yec.fresno.core.task.handler.EventListener;
import io.yec.fresno.spring.support.annotation.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.alibaba.spring.util.AnnotationUtils.getAttributes;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * {@link EventHandler} Annotation
 * {@link BeanDefinitionRegistryPostProcessor Bean Definition Registry Post Processor}
 *
 * @author baijiu.yec
 * @since 0.0.1
 */
@Slf4j
public class EventHandlerAnnotationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware {

    /**
     * @see EventHandlerAnnotationBeanPostProcessor#createEventQueueBeanDefinition(java.lang.String)
     */
    public static final String FRESNO_REDIS_TEMPLATE_BEAN_NAME = "fresnoRedisTemplate";

    public static final String EVENT_QUEUE_PROCESSING_ERROR_RETRY_COUNT_CONFIG_NAME = "fresno.poping.event.queue.processing-error-retry-count";

    public static final int EVENT_QUEUE_PROCESSING_ERROR_RETRY_COUNT_DEFAULT_VALUE = 3;

    public static final String EVENT_QUEUE_MAX_BAK_SIZE_CONFIG_NAME = "fresno.poping.event.queue.max-bak-size";

    public static final long EVENT_QUEUE_MAX_BAK_SIZE__DEFAULT_VALUE = 20000000L;

    private final Set<String> packagesToScan;

    private Environment environment;

    private ResourceLoader resourceLoader;

    private ClassLoader classLoader;

    public EventHandlerAnnotationBeanPostProcessor(String... packagesToScan) {
        this(Arrays.asList(packagesToScan));
    }

    public EventHandlerAnnotationBeanPostProcessor(Collection<String> packagesToScan) {
        this(new LinkedHashSet<>(packagesToScan));
    }

    public EventHandlerAnnotationBeanPostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        Set<String> resolvedPackagesToScan = resolvePackagesToScan(packagesToScan);

        if (!CollectionUtils.isEmpty(resolvedPackagesToScan)) {
            registerEventHandlerBeans(resolvedPackagesToScan, registry);
        } else {
            log.warn("packagesToScan is empty , EventHandlerBean registry will be ignored!");
        }

    }


    /**
     * Registers Beans whose classes was annotated {@link EventHandler}
     *
     * @param packagesToScan The base packages to scan
     * @param registry       {@link BeanDefinitionRegistry}
     */
    private void registerEventHandlerBeans(Set<String> packagesToScan, BeanDefinitionRegistry registry) {

        EventHandlerClassPathBeanDefinitionScanner scanner =
                new EventHandlerClassPathBeanDefinitionScanner(registry, environment, resourceLoader);

        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(registry);

        scanner.setBeanNameGenerator(beanNameGenerator);

        scanner.addIncludeFilter(new AnnotationTypeFilter(EventHandler.class));

        for (String packageToScan : packagesToScan) {

            // Registers @EventHandler Bean first
            scanner.scan(packageToScan);

            // Finds all BeanDefinitionHolders of @EventHandler whether @ComponentScan scans or not.
            Set<BeanDefinitionHolder> beanDefinitionHolders =
                    findEventHandlerBeanDefinitionHolders(scanner, packageToScan, registry, beanNameGenerator);

            if (!CollectionUtils.isEmpty(beanDefinitionHolders)) {
                for (BeanDefinitionHolder beanDefinitionHolder : beanDefinitionHolders) {
                    registerEventHandlerBean(beanDefinitionHolder, registry, scanner);
                }
                log.info(beanDefinitionHolders.size() + " annotated Fresno's @EventHandler Components { " +
                        beanDefinitionHolders +
                        " } were scanned under package[" + packageToScan + "]");
            } else {
                log.warn("No Spring Bean annotating Fresno's @EventHandler was found under package["
                        + packageToScan + "]");
            }

        }

    }

    /**
     * It'd better to use BeanNameGenerator instance that should reference
     * {@link ConfigurationClassPostProcessor#componentScanBeanNameGenerator},
     * thus it maybe a potential problem on bean name generation.
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @return {@link BeanNameGenerator} instance
     * @see SingletonBeanRegistry
     * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
     * @see ConfigurationClassPostProcessor#processConfigBeanDefinitions
     * @since 0.0.1
     */
    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry registry) {
        BeanNameGenerator beanNameGenerator = null;
        if (registry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry singletonBeanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGenerator = (BeanNameGenerator) singletonBeanRegistry.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
        }
        if (beanNameGenerator == null) {
            log.info("BeanNameGenerator bean can't be found in BeanFactory with name ["
                    + CONFIGURATION_BEAN_NAME_GENERATOR + "]");
            log.info("BeanNameGenerator will be a instance of " +
                    AnnotationBeanNameGenerator.class.getName() +
                    " , it maybe a potential problem on bean name generation.");
            beanNameGenerator = new AnnotationBeanNameGenerator();
        }
        return beanNameGenerator;
    }

    /**
     * Finds a {@link Set} of {@link BeanDefinitionHolder BeanDefinitionHolders} whose bean type annotated
     * {@link EventHandler} Annotation.
     *
     * @param scanner       {@link ClassPathBeanDefinitionScanner}
     * @param packageToScan pachage to scan
     * @param registry      {@link BeanDefinitionRegistry}
     * @return non-null
     * @since 2.5.8
     */
    private Set<BeanDefinitionHolder> findEventHandlerBeanDefinitionHolders(
            ClassPathBeanDefinitionScanner scanner, String packageToScan, BeanDefinitionRegistry registry,
            BeanNameGenerator beanNameGenerator) {
        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(packageToScan);
        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<>(beanDefinitions.size());
        for (BeanDefinition beanDefinition : beanDefinitions) {
            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
            beanDefinitionHolders.add(beanDefinitionHolder);
        }
        return beanDefinitionHolders;
    }

    /**
     * Registers {@link io.yec.fresno.core.task.EventHandlerBean} from new annotated {@link EventHandler} {@link BeanDefinition}
     *
     * @param beanDefinitionHolder
     * @param registry
     * @param scanner
     * @see io.yec.fresno.core.task.EventHandlerBean
     * @see BeanDefinition
     */
    private void registerEventHandlerBean(BeanDefinitionHolder beanDefinitionHolder, BeanDefinitionRegistry registry,
                                          EventHandlerClassPathBeanDefinitionScanner scanner) {

        Class<?> beanClass = resolveClass(beanDefinitionHolder);

        // valid if impl EventListener interface present, is not assert warning
        ResolvableType resolvableType = ResolvableType.forClass(EventListener.class);
        if (!resolvableType.isAssignableFrom(beanClass)) {
            throw new IllegalStateException("@EventHandler annotated class must impl EventListener interface");
        }

        Annotation eventHandler = findEventHandlerAnnotation(beanClass);

        String annotatedEventHandlerBeanName = beanDefinitionHolder.getBeanName();

        AbstractBeanDefinition eventHandlerBeanDefinition = buildEventHandlerBeanDefinition(eventHandler, annotatedEventHandlerBeanName, beanClass, registry);

        // EventHandlerBean Bean name
        String beanName = generateEventHandlerBeanName(beanClass);
        // check duplicated candidate bean
        if (scanner.checkCandidate(beanName, eventHandlerBeanDefinition)) {

            registry.registerBeanDefinition(beanName, eventHandlerBeanDefinition);

            log.info("The BeanDefinition[" + eventHandlerBeanDefinition +
                    "] of EventHandlerBean has been registered with name : " + beanName);

        } else {

            log.warn("The Duplicated BeanDefinition[" + eventHandlerBeanDefinition +
                    "] of EventHandlerBean[ bean name : " + beanName +
                    "] was be found , Did @EventHandlerComponentScan scan to same package in many times?");

        }

    }


    /**
     * Find the {@link Annotation annotation} of @EventHandler
     *
     * @param beanClass the {@link Class class} of Bean
     * @return <code>null</code> if not found
     * @since 0.0.1
     */
    private Annotation findEventHandlerAnnotation(Class<?> beanClass) {
        Annotation eventHandler = findMergedAnnotation(beanClass, EventHandler.class);
        return eventHandler;
    }

    /**
     * Generates the bean name of {@link EventHandlerBean}
     *
     * @return EventHandlerBean:eventListenerClassName
     * @since 0.0.1
     */
    private String generateEventHandlerBeanName(Class<?> eventHandlerAnnotatedClass) {
        EventHandlerBeanNameBuilder builder = EventHandlerBeanNameBuilder.create(eventHandlerAnnotatedClass, environment);
        return builder.build();
    }

    private Class<?> resolveClass(BeanDefinitionHolder beanDefinitionHolder) {
        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        return resolveClass(beanDefinition);
    }

    private Class<?> resolveClass(BeanDefinition beanDefinition) {
        String beanClassName = beanDefinition.getBeanClassName();
        return resolveClassName(beanClassName, classLoader);
    }

    private Set<String> resolvePackagesToScan(Set<String> packagesToScan) {
        Set<String> resolvedPackagesToScan = new LinkedHashSet<String>(packagesToScan.size());
        for (String packageToScan : packagesToScan) {
            if (StringUtils.hasText(packageToScan)) {
                String resolvedPackageToScan = environment.resolvePlaceholders(packageToScan.trim());
                resolvedPackagesToScan.add(resolvedPackageToScan);
            }
        }
        return resolvedPackagesToScan;
    }

    /**
     * Build the {@link AbstractBeanDefinition Bean Definition}
     *
     * @param eventHandlerAnnotation
     * @param annotatedEventHandlerBeanName
     * @return
     * @since 0.0.1
     */
    private AbstractBeanDefinition buildEventHandlerBeanDefinition(
            Annotation eventHandlerAnnotation,
            String annotatedEventHandlerBeanName,
            Class<?> eventHandlerAnnotatedClass,
            BeanDefinitionRegistry registry
    ) {
        BeanDefinitionBuilder eventHandlerBeanBuilder = rootBeanDefinition(EventHandlerBean.class);
        AbstractBeanDefinition eventHandlerBeanDefinition = eventHandlerBeanBuilder.getBeanDefinition();
        MutablePropertyValues propertyValues = eventHandlerBeanDefinition.getPropertyValues();
        propertyValues.addPropertyValues(new MutablePropertyValues(getAttributes(eventHandlerAnnotation, environment, true)));
        // References "eventHandler" property to annotated-@EventHandler Bean
        addPropertyReference(eventHandlerBeanBuilder, "eventListener", annotatedEventHandlerBeanName);

        String eventWorkerBeanName = registerEventQueueBeanDefinition(annotatedEventHandlerBeanName, eventHandlerAnnotatedClass, registry);
        addPropertyReference(eventHandlerBeanBuilder, "eventQueue", eventWorkerBeanName);
        return eventHandlerBeanBuilder.getBeanDefinition();
    }

    private String registerEventQueueBeanDefinition(String annotatedEventHandlerBeanName, Class<?> eventHandlerAnnotatedClass, BeanDefinitionRegistry registry) {
        AbstractBeanDefinition eventWorkerBeanDefinition = createEventQueueBeanDefinition(annotatedEventHandlerBeanName);
        String eventWorkerBeanName = EventQueueBeanNameBuilder.create(eventHandlerAnnotatedClass, environment).build();
        registry.registerBeanDefinition(eventWorkerBeanName, eventWorkerBeanDefinition);
        return eventWorkerBeanName;
    }

    private AbstractBeanDefinition createEventQueueBeanDefinition(String annotatedEventHandlerBeanName) {
        // 1.通过 BeanDefinitionBuilder 构建
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(EventQueue.class);
        // 通过属性设置
        beanDefinitionBuilder
                // as EventQueue's queueName
                .addConstructorArgValue(annotatedEventHandlerBeanName)
                .addPropertyValue("processingErrorRetryCount",
                        environment.getProperty(EVENT_QUEUE_PROCESSING_ERROR_RETRY_COUNT_CONFIG_NAME, Integer.class, EVENT_QUEUE_PROCESSING_ERROR_RETRY_COUNT_DEFAULT_VALUE))
                .addPropertyValue("maxBakSize",
                        environment.getProperty(EVENT_QUEUE_MAX_BAK_SIZE_CONFIG_NAME, Long.class, EVENT_QUEUE_MAX_BAK_SIZE__DEFAULT_VALUE))
                // todo a other way to find redisTemplate beanName from Fresno annotation
                .addPropertyReference("queueRedis", FRESNO_REDIS_TEMPLATE_BEAN_NAME);
        // 获取 BeanDefinition 实例
        return beanDefinitionBuilder.getBeanDefinition();
    }

    private void addPropertyReference(BeanDefinitionBuilder builder, String propertyName, String beanName) {
        String resolvedBeanName = environment.resolvePlaceholders(beanName);
        builder.addPropertyReference(propertyName, resolvedBeanName);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}