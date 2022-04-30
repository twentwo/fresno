package io.yec.fresno.spring.support.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * EnableFresno
 *
 * @author baijiu.yec
 * @since 2022/04/19
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EventHandlerComponentScan
public @interface EnableFresno {
    /**
     * Base packages to scan for annotated @Service classes.
     * <p>
     * Use {@link #scanBasePackageClasses()} for a type-safe alternative to String-based
     * package names.
     *
     * @return the base packages to scan
     * @see EventHandlerComponentScan#basePackages()
     */
    @AliasFor(annotation = EventHandlerComponentScan.class, attribute = "basePackages")
    String[] scanBasePackages() default {};

    /**
     * Type-safe alternative to {@link #scanBasePackages()} for specifying the packages to
     * scan for annotated @Service classes. The package of each class specified will be
     * scanned.
     *
     * @return classes from the base packages to scan
     * @see EventHandlerComponentScan#basePackageClasses
     */
    @AliasFor(annotation = EventHandlerComponentScan.class, attribute = "basePackageClasses")
    Class<?>[] scanBasePackageClasses() default {};
}
