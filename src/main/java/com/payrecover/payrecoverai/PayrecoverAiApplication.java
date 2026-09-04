package com.payrecover.payrecoverai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the whole backend.
 * @SpringBootApplication is a shortcut annotation that turns on:
 *  - component scanning (finds our @Service, @RestController, @Repository classes)
 *  - auto-configuration (Spring guesses sensible defaults from our dependencies)
 *  - the ability to run as a standalone app with an embedded Tomcat server
 */
@SpringBootApplication
public class PayrecoverAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayrecoverAiApplication.class, args);
    }
}
