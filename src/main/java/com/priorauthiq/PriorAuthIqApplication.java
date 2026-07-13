package com.priorauthiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PriorAuthIQ — a Spring AI reference architecture for prior-authorization
 * triage. Policy-grounded, structured, human-in-the-loop.
 */
@SpringBootApplication
public class PriorAuthIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriorAuthIqApplication.class, args);
    }
}
