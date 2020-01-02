package com.zgl.distribute.lock.aspect;

import com.zgl.distribute.lock.annotation.RedisLock;
import com.zgl.distribute.lock.service.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author zgl
 * @date 2019/12/31 下午4:39
 */
@Aspect
/*@Configuration
@ConditionalOnClass(DistributedLock.class)
@AutoConfigureAfter(DistributedLockAutoConfiguration.class)*/
@Component
@Slf4j
public class RedisDistributedLockAspect {

	@Autowired
	private DistributedLock distributedLock;

	@Pointcut("@annotation(redisLock)")
	private void lockPoint(RedisLock redisLock){

	}

	@Around("lockPoint(redisLock)")
	public Object around(ProceedingJoinPoint pjp, RedisLock redisLock) throws Throwable{
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();
		RedisLock annotation = method.getAnnotation(RedisLock.class);
		String key = annotation.value();
		if(StringUtils.isEmpty(key)){
			Object[] args = pjp.getArgs();
			key = Arrays.toString(args);
		}
		int retryTimes = annotation.action().equals(RedisLock.LockFailAction.CONTINUE) ? annotation.retryTimes() : 0;
		boolean lock = distributedLock.lock(key, annotation.keepMills(), retryTimes, annotation.sleepMills());
		if(!lock) {
			log.info("get lock failed : {}", key);
			return null;
		}

		//得到锁,执行方法，释放锁
		log.info("get lock success : {}", key);
		try {
			return pjp.proceed();
		} catch (Exception e) {
			log.error("execute locked method occurred an exception", e);
		} finally {
			boolean releaseResult = distributedLock.releaseLock(key);
			log.info("{} release lock : key:{}, releaseResult :{}", Thread.currentThread().getName(), key, releaseResult);
		}
		return null;
	}
}