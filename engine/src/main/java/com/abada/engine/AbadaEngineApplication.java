package com.abada.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AbadaEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbadaEngineApplication.class, args);
    }

}
