// src/main/java/com/yourcompany/pingpong/TabletennisApplication.java
package com.yourcompany.pingpong.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan; // ⭐⭐ import 추가 ⭐⭐

@SpringBootApplication
@ComponentScan(basePackages = "com.yourcompany.pingpong") // ⭐⭐ 이 부분이 핵심! 전체 패키지를 강제로 스캔 ⭐⭐
public class TabletennisApplication {
    public static void main(String[] args) {
        SpringApplication.run(TabletennisApplication.class, args);
    }
}