package com.pulsepass.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;


@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(
        @DefaultValue("pulsepass-api") String issuer,
        String secretBase64,
        @DefaultValue("PT15M") Duration accessTokenTtl
) {


    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", secretBase64=****, accessTokenTtl=" + accessTokenTtl + "]";
    }
}
