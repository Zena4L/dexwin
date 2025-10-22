package com.clement.dexwin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ConfigurationProperties(prefix = "rsa")
public record RsaProperties(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
}