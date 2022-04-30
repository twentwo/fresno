package io.yec.fresno.spring.support.annotation;

import io.yec.fresno.spring.support.config.EventHandlerComponentScanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * EventHandler Component Scan {@link Annotation},scans the classpath for annotated components that will be auto-registered as
 * Spring beans. Fresno-provided {@link EventHandler}.
 *
 * @author baijiu.yec
 * @see EventHandler
 * @since 0.0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EventHandlerComponentScanRegistrar.class)
public @interface EventHandlerComponentScan {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation
     * declarations e.g.: {@code @EventHandlerComponentScan("org.my.pkg")} instead of
     * {@code @EventHandlerComponentScan(basePackages="org.my.pkg")}.
     *
     * @return the base packages to scan
     */
    String[] value() default {};

    /**
     * Base packages to scan for annotated @Service classes. {@link #value()} is an
     * alias for (and mutually exclusive with) this attribute.
     * <p>
     * Use {@link #basePackageClasses()} for a type-safe alternative to String-based
     * package names.
     *
     * @return the base packages to scan
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to
     * scan for annotated @Service classes. The package of each class specified will be
     * scanned.
     *
     * @return classes from the base packages to scan
     */
    Class<?>[] basePackageClasses() default {};

}
