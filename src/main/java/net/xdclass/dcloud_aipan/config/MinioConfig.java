package net.xdclass.dcloud_aipan.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    private String endpoint;
    private String accessKey;
    private String accessSecret;
    private String bucketName;


    // 预签名的URL 过期时间
    private Long PRE_SIGN_URL_EXPIRE_TIME = 60 * 10 * 1000L;

    @Bean
    public MinioClient getMinioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, accessSecret)
                .build();
    }
}
