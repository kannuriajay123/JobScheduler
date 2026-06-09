package com.example.jobqueue.config;

import org.springframework.context.annotation.Configuration;

/**
 * Central configuration for service beans and dependencies
 * Implements Dependency Injection pattern: manages bean lifecycle and wiring
 * Allows for clean separation of configuration from business logic
 */
@Configuration
public class JobServiceConfiguration {
    // All beans are auto-discovered via @Component, @Service, @Repository annotations
    // No additional configuration needed - Spring handles wiring automatically
}
