package org.app.mintonmatchapi;

import org.app.mintonmatchapi.config.LocalH2ServerStarter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MintonMatchApiApplication {

    public static void main(String[] args) {
        LocalH2ServerStarter.startIfLocal(args);
        SpringApplication.run(MintonMatchApiApplication.class, args);
    }

}
