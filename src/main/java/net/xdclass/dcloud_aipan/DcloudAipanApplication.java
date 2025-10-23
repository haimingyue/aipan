package net.xdclass.dcloud_aipan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class DcloudAipanApplication {

	public static void main(String[] args) {
		SpringApplication.run(DcloudAipanApplication.class, args);
	}

}
