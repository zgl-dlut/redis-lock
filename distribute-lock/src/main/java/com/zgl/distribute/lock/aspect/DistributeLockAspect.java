package com.zgl.distribute.lock.aspect;

import com.zgl.distribute.lock.annotation.DistributeLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


/**
 * @author zgl
 * @date 2019/10/31 下午7:33
 */
@Slf4j
@Aspect
@Component
public class DistributeLockAspect {

	@Autowired
	private RedissonClient redissonClient;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Pointcut("@annotation(distributeLock)")
	public void distributeLockAspect(DistributeLock distributeLock) {}

	@Around(value = "distributeLockAspect(distributeLock)")
	public void doAround(ProceedingJoinPoint pjp, DistributeLock distributeLock) throws Throwable {
		log.info("Lock before");
		DistributeLock annotation = ((MethodSignature)pjp.getSignature()).getMethod().getAnnotation(DistributeLock.class);
		String lockKey = annotation.lockKey();
		if (StringUtils.isBlank(lockKey)) {
			throw new IllegalArgumentException("Lock key must not be empty");
		}
		RLock rLock = redissonClient.getLock(lockKey);
		try {
			rLock.lock(distributeLock.leaseTime(), distributeLock.unit());
			log.info("+++++++++++++++:{}", redisTemplate.opsForHash().entries(lockKey));
			pjp.proceed();
		} finally {
			try {
				log.info("Unlock after");
				TimeUnit.SECONDS.sleep(3L);
				rLock.unlock();
			} catch (Exception e) {
				log.error("Redisson RLock unlock error", e);
			}
		}
	}

}
