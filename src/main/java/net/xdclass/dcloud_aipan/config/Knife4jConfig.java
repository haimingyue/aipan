package net.xdclass.dcloud_aipan.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class Knife4jConfig {


    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("网盘系统 API")
                        .version("1.0")
                        .description("AI网盘系统")
                        .termsOfService("https://tlpy8.com")
                        .license(new License().name("Apache 2.0").url("https://tlpy8.com"))
                        // 添加作者信息
                        .contact(new Contact()
                                .name("Simoon") // 替换为作者的名字
                                .email("420526391@qq.com") // 替换为作者的电子邮件
                                .url("https://tlpy8.com") // 替换为作者的网站或个人资料链接
                        )
                ) ;
    }

}