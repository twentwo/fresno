package io.yec.fresno.spring.boot.autoconfigure.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.LinkedHashSet;
import java.util.Set;

import static io.yec.fresno.spring.boot.autoconfigure.utils.FresnoUtils.FRESNO_PREFIX;

/**
 * FresnoConfigProperties
 *
 * @author baijiu.yec
 * @since 2022/04/29
 */
@ConfigurationProperties(prefix = FRESNO_PREFIX)
public class FresnoConfigProperties {

    @NestedConfigurationProperty
    @Setter
    @Getter
    private Scan scan = new Scan();

    public static class Scan {
        /**
         * The basePackages to scan , the multiple-value is delimited by comma
         *
         * @see io.yec.fresno.spring.support.annotation.EnableFresno#scanBasePackages()
         */
        private Set<String> basePackages = new LinkedHashSet<>();

        public Set<String> getBasePackages() {
            return basePackages;
        }

        public void setBasePackages(Set<String> basePackages) {
            this.basePackages = basePackages;
        }
    }

}
