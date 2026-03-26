package org.app.mintonmatchapi;

import org.app.mintonmatchapi.config.MintonWebProperties;
import org.app.mintonmatchapi.config.StompRedisRelayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({StompRedisRelayProperties.class, MintonWebProperties.class})
public class MintonMatchApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MintonMatchApiApplication.class, args);
    }

}
