package com.example.blog_froum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.blog_froum.mapper")
public class BlogFroumApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogFroumApplication.class, args);
    }

}
