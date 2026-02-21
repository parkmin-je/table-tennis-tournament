// src/main/java/com/yourcompany/pingpong/TabletennisApplication.java
package com.yourcompany.pingpong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.yourcompany.pingpong.domain")
@EnableJpaRepositories(basePackages = "com.yourcompany.pingpong.modules")  // ⭐ modules 전체 스캔!
public class TabletennisApplication {
    public static void main(String[] args) {
        SpringApplication.run(TabletennisApplication.class, args);
    }
}