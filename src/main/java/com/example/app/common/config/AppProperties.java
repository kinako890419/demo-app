package com.example.app.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Public host of this service, used to build SSO callback URLs.
 * {@code localBaseUrl} is used instead when a login request passes {@code isLocalDev=true}.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private String baseUrl = "http://localhost:8181";
    private String localBaseUrl = "http://localhost:8181";

    public String getBaseUrl(boolean isLocalDev) {
        return isLocalDev ? localBaseUrl : baseUrl;
    }
}
