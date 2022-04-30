package io.yec.fresno.spring.boot.autoconfigure.properties;

import io.yec.fresno.spring.support.config.EventHandlerAnnotationBeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static io.yec.fresno.spring.boot.autoconfigure.utils.FresnoUtils.FRESNO_EVENT_PREFIX;

/**
 * FresnoConfigConfiguration
 *
 * @author baijiu.yec
 * @since 2022/04/21
 */
@ConfigurationProperties(prefix = FRESNO_EVENT_PREFIX)
public class FresnoEventConfigProperties {

    @NestedConfigurationProperty
    private Worker worker = new Worker();

    @NestedConfigurationProperty
    private Queue queue = new Queue();

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Queue getQueue() {
        return queue;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public static class Worker {

        private Integer ringBufferSize = 4096;
        private Integer threadPoolSize = 256;

        public Integer getRingBufferSize() {
            return ringBufferSize;
        }

        public void setRingBufferSize(Integer ringBufferSize) {
            this.ringBufferSize = ringBufferSize;
        }

        public Integer getThreadPoolSize() {
            return threadPoolSize;
        }

        public void setThreadPoolSize(Integer threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
        }
    }

    public static class Queue {

        private Integer processingErrorRetryCount = EventHandlerAnnotationBeanPostProcessor.EVENT_QUEUE_PROCESSING_ERROR_RETRY_COUNT_DEFAULT_VALUE;
        private Long maxBakSize = EventHandlerAnnotationBeanPostProcessor.EVENT_QUEUE_MAX_BAK_SIZE__DEFAULT_VALUE;

        public Integer getProcessingErrorRetryCount() {
            return processingErrorRetryCount;
        }

        public void setProcessingErrorRetryCount(Integer processingErrorRetryCount) {
            this.processingErrorRetryCount = processingErrorRetryCount;
        }

        public Long getMaxBakSize() {
            return maxBakSize;
        }

        public void setMaxBakSize(Long maxBakSize) {
            this.maxBakSize = maxBakSize;
        }

    }

}
