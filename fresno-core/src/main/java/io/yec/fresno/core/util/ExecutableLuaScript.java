package io.yec.fresno.core.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * io.yec.fresno.core.util.ExecutableLuaScript
 *
 * @author yecong
 * @date 2018/09/11
 */
public class ExecutableLuaScript<T> {

    private RedisScript<T> redisScript;

    public ExecutableLuaScript() {
    }

    public ExecutableLuaScript(String scriptText) {
        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(scriptText);
        this.redisScript = redisScript;
    }

    public ExecutableLuaScript(String scriptText, Class<T> returnType) {
        this.redisScript = new DefaultRedisScript<>(scriptText, returnType);
    }

    public T exec(RedisTemplate redisTemplate, List keys, Object... args) {
        return (T) redisTemplate.execute(redisScript, keys, args);
    }

}
