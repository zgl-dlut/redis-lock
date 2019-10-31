package com.zgl.distribute.lock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
public class DistributeLockApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributeLockApplication.class, args);
	}

}
