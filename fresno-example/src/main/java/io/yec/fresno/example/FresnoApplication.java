package io.yec.fresno.example;

import io.yec.fresno.example.dto.Order;
import io.yec.fresno.example.dto.User;
import io.yec.fresno.example.service.FooService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.util.StopWatch;

@Slf4j
@SpringBootApplication
public class FresnoApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(FresnoApplication.class, args);

        FooService fooService = applicationContext.getBean(FooService.class);

        fooService.putUser(User.createUser(1L, "yec", "123"));

        StopWatch stopWatch = new StopWatch("fooService.enQueueOrder");
        stopWatch.start();
        for (int i = 0; i < 100000; i++) {
            long current = System.currentTimeMillis();
            fooService.enQueueOrder(Order.createOrder(new Long(i) + current, "orderNo-" + (i + current),
                    "title-" + (i + current)));
        }
        stopWatch.stop();
        log.info("fooService.enQueueOrder finish {}", stopWatch.getTotalTimeMillis());

    }

    @Bean("txManager")
    public PlatformTransactionManager txManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) throws TransactionException {
                System.out.println("txManager : transaction commit...");
            }

            @Override
            public void rollback(TransactionStatus status) throws TransactionException {
                System.out.println("txManager : transaction rollback...");
            }
        };
    }

}
