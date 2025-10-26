package net.xdclass.dcloud_aipan.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmazonS3Config {

    @Resource
    private MinioConfig minioConfig;

    @Bean(name="amazonS3Client")
    public AmazonS3 amazonS3Client() {
        // 创建客户端配置
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        // 配置协议
        clientConfiguration.setProtocol(Protocol.HTTP);
        // 配置超时时间
        clientConfiguration.setConnectionTimeout(5000);
        clientConfiguration.setUseExpectContinue(true);

        // 使用 Minio 配置的密钥创建访问的凭证
        AWSCredentials credentials = new BasicAWSCredentials(minioConfig.getAccessKey(), minioConfig.getAccessSecret());
        // 设置 Endpoint
        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                minioConfig.getEndpoint(),
                Regions.CN_NORTH_1.getName()
        );

        // 使用上面的配置创建客户端
        return AmazonS3ClientBuilder.standard().withClientConfiguration(clientConfiguration)
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }
}
