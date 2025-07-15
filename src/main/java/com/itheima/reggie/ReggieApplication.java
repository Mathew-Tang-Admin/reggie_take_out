package com.itheima.reggie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author MathewTang
 */
@ServletComponentScan
@Slf4j
@SpringBootApplication
@EnableTransactionManagement // 开启事务注解的支持
@EnableCaching // 开启缓存注解功能
public class ReggieApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReggieApplication.class, args);
        log.info("项目启动成功...");
    }
}

// http://localhost:8080/front/page/login.html
// http://localhost:8080/backend/index.html

/*
【黑马程序员Java项目实战《瑞吉外卖》，轻松掌握springboot + mybatis plus开发核心技术的真java实战项目】https://www.bilibili.com/video/BV13a411q753?p=82&vd_source=339999fb6210f92ee0f3e3bf990e51b2
短信服务 将P82 day5-14
阿里云 个人无法申请
 */