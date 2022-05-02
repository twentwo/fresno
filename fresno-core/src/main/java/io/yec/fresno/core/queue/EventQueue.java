package io.yec.fresno.core.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.yec.fresno.core.util.ExecutableLuaScript;
import io.yec.fresno.core.util.GetIpAddress;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * io.yec.fresno.core.queue.EventQueue
 *
 * @author yecong
 * @date 2018/07/31
 */
@Slf4j
@ToString(exclude = {"queueRedis", "lock", "notEmpty"})
@NoArgsConstructor
public class EventQueue<T> {

    public static final long DEFAULT_AWAIT_IN_MILLIS = 500;

    private static final ExecutableLuaScript ADD_TO_BACK_REDIS_SCRIPT = new ExecutableLuaScript(
            "redis.call('lpush', KEYS[2], ARGV[1]) " +
                    "redis.call('lrem', KEYS[1], 0, ARGV[1])");

    private static final ExecutableLuaScript ADD_TO_FAIL_QUEUE_REDIS_SCRIPT = new ExecutableLuaScript(
            "redis.call('lpush', KEYS[2], ARGV[1]) " +
                    "redis.call('lrem', KEYS[1], 0, ARGV[1]) " +
                    "redis.call('del', KEYS[3])");

    private static final ExecutableLuaScript ENQUEUE_TO_LEFT_REDIS_SCRIPT = new ExecutableLuaScript(
            "redis.call('lpush', KEYS[1], ARGV[1]) " +
                    " if tonumber(ARGV[2]) <=0 then return nil end " +
                    " local len = redis.call('llen', KEYS[2]) " +
                    " if len >= tonumber(ARGV[2]) then redis.call('rpop', KEYS[2]) end " +
                    " redis.call('lpush', KEYS[2], ARGV[1])"
    );

    @Setter
    private RedisTemplate<String, T> queueRedis;

    @Getter
    @Setter
    private String queueName;
    private String processingQueueName;
    private String failedQueueName;
    private String bakQueueName;
    @Setter
    private long maxBakSize;

    private long awaitInMillis = DEFAULT_AWAIT_IN_MILLIS;
    @Setter
    private int processingErrorRetryCount;

    private Lock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();

    public EventQueue(
        String queueName,
        RedisTemplate<String, T> queueRedis,
        int processingErrorRetryCount,
        long maxBakSize
    ) {
        this(queueName);
        this.queueRedis = queueRedis;
        this.processingErrorRetryCount = processingErrorRetryCount;
        this.maxBakSize = maxBakSize;
    }

    public EventQueue(String queueName) {
        this.queueName = queueName;
        this.processingQueueName = queueName + "_processing_queue_" + GetIpAddress.getLinuxLocalIp();
        this.failedQueueName = queueName + "_failed_queue";
        this.bakQueueName = queueName + "_bak_queue_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
    }

    public T next() {
        while (true) {
            // 1. 暂停Queue消费 todo
            T obj = null;
            try {
                // 2. 从等待 Queue POP, 然后 PUSH 到本地处理队列
                obj = queueRedis.opsForList().rightPopAndLeftPush(queueName, processingQueueName);
            } catch (Exception e) {
                // 3. 发生了网络异常后警告，然后人工或定期检测
                // 将本地任务队列长时间未消费的任务推送回等待队列
                continue;
            }

            //4. 返回获取的任务
            if (obj != null) {
                return obj;
            }
            lock.lock();
            try {
                // 如果没有任务, 则休息一下稍后处理, 防止死循环耗死CPU
                if (awaitInMillis < 1000) {
                    awaitInMillis = awaitInMillis + awaitInMillis;
                }
                notEmpty.await(awaitInMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                //ignore
            } finally {
                lock.unlock();
            }
        }
    }

    public void success(T obj) {
        log.debug("操作成功, {}", obj);
        queueRedis.opsForList().remove(processingQueueName, 0, obj);
        log.debug("从处理队列删除成功, {}", obj);
        // 删除失败记录
        String failCountKey = getFailCountName(obj);
        if (queueRedis.opsForValue().get(failCountKey) != null) {
            queueRedis.delete(failCountKey);
        }
    }

    public void fail(T obj) {
        log.debug("操作失败, {}", obj);
        String failCountKey = getFailCountName(obj);
        final int failedCount = queueRedis.opsForValue().increment(failCountKey, 1).intValue();
        if (failedCount <= processingErrorRetryCount) {
            // 如果小于等于重试次数，则直接添加到等待队列队尾
            log.error("未超出重试次数{}, 插入等待队列队尾", processingErrorRetryCount);
            // leftPush queueName, remove processingQueueName
            ADD_TO_BACK_REDIS_SCRIPT.exec(queueRedis, Lists.newArrayList(processingQueueName, queueName), obj);
        } else {
            // 如果超过失败重试次数, 则加入失败队列
            log.error("超出重试次数{}, 插入失败队列", processingErrorRetryCount);
            // leftPush failedQueueName, remove processingQueueName, delete failCountKey
            ADD_TO_FAIL_QUEUE_REDIS_SCRIPT.exec(queueRedis, Lists.newArrayList(processingQueueName, failedQueueName, failCountKey), obj);
        }
    }

    /**
     * 接收其他系统推送的任务
     * @param obj
     */
    public void enqueueToBack(T obj) {
        ENQUEUE_TO_LEFT_REDIS_SCRIPT.exec(queueRedis, Lists.newArrayList(queueName, bakQueueName), obj, maxBakSize);
    }

    private String getFailCountName(T obj) {
        String md5 = null;
        try {
            md5 = DigestUtils.md5Hex(new ObjectMapper().writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new StringBuffer("failed:").append(md5).append(":count").toString();
    }

}
