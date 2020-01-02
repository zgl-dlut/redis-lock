package com.zgl.distribute.lock.service;

import com.zgl.distribute.lock.annotation.DistributeLock;
import com.zgl.distribute.lock.annotation.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zgl
 * @date 2019/10/31 下午7:52
 */
@Component
@Slf4j
public class Service {

	private AtomicInteger atomicInteger = new AtomicInteger(500);
	private int i = 500;

	//@DistributeLock(lockKey = "zgl")
	@RedisLock(value = "zgl")
	public void test() {
		log.info(Thread.currentThread().getName() + "获得了锁");
		log.info("{}=================atomicInteger:{}=======i:{}", Thread.currentThread().getName(), atomicInteger.getAndDecrement(), i--);

	}
}