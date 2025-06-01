package com.web;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = MybatisPlusAutoConfiguration.class)
@MapperScan("com.web.mapper")  // 指定 Mapper 接口所在的包路径
public class WeebApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeebApplication.class, args);
	}

}
