package io.yec.fresno.core.task.proxy;

import io.yec.fresno.core.Event;
import io.yec.fresno.core.queue.EventQueue;
import io.yec.fresno.core.task.handler.EventListener;
import lombok.Setter;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.beans.BeansException;

/**
 * {@link EventListener#onEvent(EventQueue, Event)} proxy creator
 *
 * @author baijiu.yec
 * @since 2022/05/01
 */
public class OnEventProxyCreator extends AbstractAutoProxyCreator {

    @Setter
    private OnEventProxyAdvice onEventProxyAdvice;

    public OnEventProxyCreator(OnEventProxyAdvice onEventProxyAdvice) {
        this.onEventProxyAdvice = onEventProxyAdvice;
        setProxyTargetClass(true);
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException {
        if (EventListener.class.isAssignableFrom(beanClass)) {
            // DefaultIntroductionAdvisor type filter
            return new Object[]{new DefaultIntroductionAdvisor(onEventProxyAdvice)};
        }
        return DO_NOT_PROXY;
    }

}
