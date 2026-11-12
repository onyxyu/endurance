package io.github.onyxyu.endurance.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Endurance Harness 的启动与装配入口。
 */
@SpringBootApplication
public class EnduranceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnduranceApplication.class, args);
    }
}
