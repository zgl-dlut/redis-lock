package com.zgl.distribute.lock.controller;

import com.zgl.distribute.lock.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author zgl
 * @date 2019/10/31 下午7:55
 */
@RestController
@RequestMapping("/distribute")
public class TestController {

	@Autowired
	private Service service;

	@GetMapping("/lock")
	public void test() {
		for (int i = 0; i < 5; i++) {
			new Thread(() -> service.test()).start();
		}
	}
}