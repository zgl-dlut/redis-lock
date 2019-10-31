package com.zgl.distribute.lock.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author zgl
 * @date 2019/10/31 下午7:24
 */
@Data
@ConfigurationProperties(prefix = "redisson")
public class RedissonProperties {

	private int timeout;

	private String address;

	private String password;

	private int connectionPoolSize;

	private int connectionMinimumIdleSize;
}