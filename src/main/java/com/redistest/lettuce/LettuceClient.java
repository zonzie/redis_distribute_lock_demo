package com.redistest.lettuce;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;

/**
 * @author zonzie
 * @date 2018/8/10 17:25
 */
@RestController
@RequestMapping
public class LettuceClient {

    @Resource(name = "lettuceConnect")
    private StatefulRedisConnection<String, String> connection;

    private static final int DEFAULT_TIME = 1000;
    private static final String LUA_SCRIPT = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";

    /**
     * 同步set
     * @param key
     * @param value
     */
    @GetMapping("lettuceSet")
    public String lettuceSet(String key, String value) {
        RedisCommands<String, String> commands = connection.sync();
        SetArgs px = SetArgs.Builder.nx().px(5000);
        String set = commands.set(key, value, px);
        return set;
    }

    /**
     * 同步get
     * @param key
     * @return
     */
    @GetMapping("lettuceGet")
    public String lettuceGet(String key) {
        RedisCommands<String, String> sync = connection.sync();
        String s = sync.get(key);
        return s;
    }

    /**
     * 异步set
     * @param key
     * @param value
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping("asyncSet")
    public String lettuceAsyncSet(String key, String value) throws ExecutionException, InterruptedException {
        RedisAsyncCommands<String, String> async = connection.async();
        // 加锁
        RedisFuture<String> set = async.set(key, value, SetArgs.Builder.nx().px(10000));
        String s = set.get();
        System.out.println(s);
        return s;
    }

    /**
     * 阻塞锁
     * @param key
     * @param value
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping(value = "lettuceTryLock")
    public String tryLock(String key, String value) throws ExecutionException, InterruptedException {
        // 自定义阻塞的时间
        int blockTIme = 5000;
        RedisAsyncCommands<String, String> async = connection.async();
        for (;;) {
            if(blockTIme >= 0) {
                // 加锁
                RedisFuture<String> set = async.set(key, value, SetArgs.Builder.nx().px(10000));
                String s = set.get();
                System.out.println(s == null ? "waiting..." : s);
                if("OK".equalsIgnoreCase(s)) {
                    break;
                }
                blockTIme -= DEFAULT_TIME;
                Thread.sleep(DEFAULT_TIME);
            } else {
                System.out.println("over blocktime....");
                break;
            }
        }
        return "success";
    }

    /**
     * 解锁
     * @param key
     * @param value
     */
    @GetMapping("lettuceUnLock")
    public Object unlock(String key, String value) throws ExecutionException, InterruptedException {

        RedisAsyncCommands<String, String> async = connection.async();
        String[] strings = {key};
        RedisFuture<Long> eval = async.eval(LUA_SCRIPT, ScriptOutputType.INTEGER, strings, value);
        Long aLong = eval.get();
        System.out.println("解锁结果-result: " + aLong);
        return aLong;
    }

    /**
     * 异步get
     * @param key
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping("asyncGet")
    public String lettuceAsyncGet(String key) throws ExecutionException, InterruptedException {
        RedisAsyncCommands<String, String> async = connection.async();
        RedisFuture<String> stringRedisFuture = async.get(key);
        String s = stringRedisFuture.get();
        System.out.println(s);
        return s;
    }

}
