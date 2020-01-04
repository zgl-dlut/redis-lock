package com.zgl.distribute.lock.service.impl;

import jodd.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

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

	public static ThreadPoolExecutor retryPoolExecutor = new ThreadPoolExecutor(10, 200, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1024));

	public RedisDistributedLock(RedisTemplate<Object, Object> redisTemplate) {
		super();
		this.redisTemplate = redisTemplate;
	}

	@Override
	public boolean lock(String key, long expire, int retryTimes, long sleepMillis) {
		/*lockFlag.set(UUID.randomUUID().toString();
		boolean result = setRedis(key, expire);
		if (result) {
			return true;
		}
		int[] retryNum = new int[1];
		retryNum[0] = retryTimes;
		// 如果获取锁失败，按照传入的重试次数进行重试
		//TODO 建议异步轮询
		Future<Boolean> futureResult = retryPoolExecutor.submit(() -> {
			boolean finalResult = false;
			while ((!finalResult) && retryNum[0]-- > 0) {
				try {
					TimeUnit.MILLISECONDS.sleep(sleepMillis);
					log.info("lock failed, retrying...{}, retry thread:{}", retryNum[0], Thread.currentThread().getName());
				} catch (InterruptedException e) {
					return false;
				}
				lockFlag.set(UUID.randomUUID().toString());
				finalResult = setRedis(key, expire);
			}
			return finalResult;
		});
		try {
			return futureResult.get();
		} catch (Exception e) {
			return false;
		}*/
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
			/**
			 * 整合setNX命令和expire命令,使其是一个原子操作,解决了由于setNX成功之后expire开始之前发生宕机导致分布式锁永远不释放的问题
			 */
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
			/**
			 * 执行lua脚本,删除key的时候,需要判断当前key和对应的value是一个,才进行删除
			 * 如果超时后业务逻辑还没执行完,可以下面的命令可以避免误删其他线程的锁
			 * Redis锁的过期时间小于业务的执行时间该如何续期?
			 */
			Long result = redisTemplate.execute(lockScript, Collections.singletonList(key), lockFlag.get());
			return result == 1L;

		} catch (Exception e) {
			log.error("release lock occurred an exception:{}", e);
		}
		return false;
	}

	/**
	 * 获取一个redis分布锁
	 *
	 * @param lockKey        锁住的key
	 * @param lockExpireMils 锁住的时长。如果超时未解锁，视为加锁线程死亡，其他线程可夺取锁
	 * @return
	 */
	public boolean lock1(String lockKey, long lockExpireMils) {
		return (Boolean) redisTemplate.execute((RedisCallback) connection -> {
			long nowTime = System.currentTimeMillis();
			/**
			 * setNX value为当前时间 + 持锁时间
			 */
			Boolean acquire = connection.setNX(lockKey.getBytes(), String.valueOf(nowTime + lockExpireMils + 1).getBytes());
			if (acquire) {
				/**
				 * 设置成功, 返回true
				 */
				return Boolean.TRUE;
			} else {
				byte[] value = connection.get(lockKey.getBytes());
				if (Objects.nonNull(value) && value.length > 0) {
					long oldTime = Long.parseLong(new String(value));
					nowTime = System.currentTimeMillis();
					if (oldTime < nowTime) {
						/**
						 * 过期时间小于当前时间,设置新的过期时间
						 * connection.getSet：返回这个key的旧值并设置新值。
						 */
						byte[] oldValue = connection.getSet(lockKey.getBytes(), String.valueOf(nowTime + lockExpireMils + 1).getBytes());
						nowTime = System.currentTimeMillis();
						/**
						 * 当key不存时会返回空，表示key不存在或者已在管道中使用
						 */
						return oldValue == null ? false : Long.parseLong(new String(oldValue)) < nowTime;
					}
				}
			}
			/**
			 * 过期时间大于当前时间, 返回失败(设置失败, 不需要续期)
			 */
			return Boolean.FALSE;
		});
	}
}