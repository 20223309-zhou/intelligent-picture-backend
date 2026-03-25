package com.picture.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.picture.backend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class IntelligentPictureBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntelligentPictureBackendApplication.class, args);
    }

}
