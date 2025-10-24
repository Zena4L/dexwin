package com.clement.dexwin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppVariable (
    List<String> allowedOrigins,
    List<String> allowedMethods,
    List<String> allowedHeaders){
}
