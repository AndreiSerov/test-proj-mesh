package com.example.testprojmesh.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.interest")
public record InterestProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("PT30S") Duration interval,
    @DefaultValue("1.10") BigDecimal growth,
    @DefaultValue("2.07") BigDecimal capFactor) {
}
