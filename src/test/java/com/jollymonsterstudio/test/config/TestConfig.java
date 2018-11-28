package com.jollymonsterstudio.test.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * TestConfig - primarily here to bring in the test properties
 */
@Configuration
@PropertySource("test-config.properties")
@ComponentScan("com.jollymonsterstudio.unreal")
public class TestConfig {
}
