package io.yec.fresno.spring.boot.autoconfigure.utils;

import io.yec.fresno.spring.boot.autoconfigure.FresnoAutoConfiguration;

import java.util.Set;

/**
 * fresno utils
 *
 * @author baijiu.yec
 * @since 2022/04/29
 */
public class FresnoUtils {

    /**
     * The separator of property name
     */
    public static final String PROPERTY_NAME_SEPARATOR = ".";

    /**
     * The prefix of property name of Fresno
     */
    public static final String FRESNO_PREFIX = "fresno.poping";

    /**
     * The prefix of property name for Fresno event
     */
    public static final String FRESNO_EVENT_PREFIX = FRESNO_PREFIX + PROPERTY_NAME_SEPARATOR + "event";

    /**
     * The prefix of property name for Fresno scan
     */
    public static final String FRESNO_SCAN_PREFIX = FRESNO_PREFIX + PROPERTY_NAME_SEPARATOR + "scan" + PROPERTY_NAME_SEPARATOR;

    /**
     * The property name of base packages to scan
     * <p>
     * The default value is empty set.
     */
    public static final String BASE_PACKAGES_PROPERTY_NAME = "base-packages";

    /**
     * The bean name of {@link Set} presenting {@link io.yec.fresno.spring.support.config.EventHandlerAnnotationBeanPostProcessor}'s base-packages
     *
     * @since 0.0.1
     */
    public static final String BASE_PACKAGES_BEAN_NAME = "fresno-event-handler-class-base-packages";

    /**
     * The bean name of fresno ObjectMapper
     * @see FresnoAutoConfiguration#fresnoObjectMapper()
     */
    public static final String FRESNO_OBJECT_MAPPER_NAME = "fresno-object-mapper";

}
