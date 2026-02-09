package com.liubinrui;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.liubinrui.mapper")
public class LldatiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LldatiApplication.class, args);
    }

}
