package com.myy.weitutravel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan(basePackages = {"com.myy.weitutravel.**.mapper"})
public class WeituTravelBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeituTravelBackendApplication.class, args);
    }

}
