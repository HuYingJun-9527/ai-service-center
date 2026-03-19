package com.hyj.aicodehelper.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 创建MultipartFile的Schema
        Schema<?> fileSchema = new Schema<>()
                .type("string")
                .format("binary")
                .description("文件上传");

        return new OpenAPI()
                .info(new Info()
                        .title("AI Code Helper API")
                        .description("AI编程助手API文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI Code Helper Team")
                                .email("support@aicodehelper.com")
                                .url("https://aicodehelper.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components()
                        .addSchemas("MultipartFile", fileSchema));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }
}