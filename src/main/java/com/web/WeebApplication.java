package com.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@MapperScan("com.web.mapper")
public class WeebApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeebApplication.class, args);
	}

}
