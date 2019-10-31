package com.zgl.distribute.lock.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributeLock {

	String lockKey();

	long leaseTime() default 10;

	TimeUnit unit() default TimeUnit.SECONDS;
}
