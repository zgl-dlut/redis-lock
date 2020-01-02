package com.zgl.distribute.lock.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author zgl
 * @date 2019/12/31 下午4:19
 */
@Slf4j
@Component
public class RedisDistributedLock extends AbstractDistributedLock {

	@Autowired
	private RedisTemplate<Object, Object> redisTemplate;

	private ThreadLocal<String> lockFlag = new ThreadLocal<>();

	private DefaultRedisScript<Long> lockScript;

	public static final String UNLOCK_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";


	public RedisDistributedLock(RedisTemplate<Object, Object> redisTemplate) {
		super();
		this.redisTemplate = redisTemplate;
	}

	@Override
	public boolean lock(String key, long expire, int retryTimes, long sleepMillis) {
		boolean result = setRedis(key, expire);
		// 如果获取锁失败，按照传入的重试次数进行重试
		while((!result) && retryTimes-- > 0){
			try {
				log.info("lock failed, retrying...{}", retryTimes);
				Thread.sleep(sleepMillis);
			} catch (InterruptedException e) {
				return false;
			}
			result = setRedis(key, expire);
		}
		return result;
	}

	private Boolean setRedis(String key, long expire) {
		try {
			String uuid = UUID.randomUUID().toString();
			lockFlag.set(uuid);
			log.info("{} key:{}, value:{}", Thread.currentThread().getName(), key, uuid);
			Boolean result = redisTemplate.opsForValue().setIfAbsent(key, uuid, expire, TimeUnit.SECONDS);
			log.info("{} key:{}, value:{}, setNX status:{}", Thread.currentThread().getName(), key, uuid, result);
			return result;
		} catch (Exception e) {
			log.error("set redis occurred an exception:{}", e);
		}
		return false;
	}

	@Override
	public boolean releaseLock(String key) {
		try {
			lockScript = new DefaultRedisScript<>();
			lockScript.setScriptText(UNLOCK_LUA);
			lockScript.setResultType(Long.class);
			Long result = redisTemplate.execute(lockScript, Collections.singletonList(key), lockFlag.get());
			return result == 1L;

		} catch (Exception e) {
			log.error("release lock occurred an exception:{}", e);
		}
		return false;
	}
}