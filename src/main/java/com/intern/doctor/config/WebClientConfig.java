package com.intern.doctor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 配置：不固定 baseUrl（GitHub 和 Gitee 的 API 根地址不同），
 * 认证头在每个请求里按平台动态添加——见 RepoApiClient。
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient repoWebClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader(HttpHeaders.USER_AGENT, "git-doctor-intern-test")
                .build();
    }
}
