package com.priorauthiq.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a request references a service code with no coverage policy on
 * file. Maps to HTTP 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(String serviceCode) {
        super("No coverage policy found for service code: " + serviceCode);
    }
}
