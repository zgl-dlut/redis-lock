/*
package com.zgl.distribute.lock.configuration;

import com.zgl.distribute.lock.service.DistributedLock;
import com.zgl.distribute.lock.service.impl.RedisDistributedLock;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

*/
/**
 * @author zgl
 * @date 2019/12/31 下午4:37
 *//*

@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class DistributedLockAutoConfiguration {

	@Bean
	@ConditionalOnBean(RedisTemplate.class)
	public DistributedLock redisDistributedLock(RedisTemplate<Object, Object> redisTemplate){
		return new RedisDistributedLock(redisTemplate);
	}
}*/
