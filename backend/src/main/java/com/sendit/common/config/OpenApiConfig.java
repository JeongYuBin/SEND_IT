package com.sendit.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI senditOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SEND IT API")
                .description("관광 콘텐츠 저장 및 여행 경로 생성 서비스 API")
                .version("v1"));
    }
}

