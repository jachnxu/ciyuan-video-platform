package com.ciyuan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.ciyuan.mapper")
public class CiyuanApplication {
    public static void main(String[] args) {
        SpringApplication.run(CiyuanApplication.class, args);
    }
}
