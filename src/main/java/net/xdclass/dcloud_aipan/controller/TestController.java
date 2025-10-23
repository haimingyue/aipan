package net.xdclass.dcloud_aipan.controller;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import net.xdclass.dcloud_aipan.config.MinioConfig;
import net.xdclass.dcloud_aipan.util.CommonUtil;
import net.xdclass.dcloud_aipan.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/test/v1")
public class TestController {
    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private MinioClient minioClient;

    @PostMapping("upload")
    public JsonData testUpload(@RequestParam("file") MultipartFile file) {

        String filePath = CommonUtil.getFilePath(file.getOriginalFilename());

        // 读取文件流，上传到 minio存储桶中
        try {
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(PutObjectArgs.builder().bucket(minioConfig.getBucketName())
                    .object(filePath)
                    .stream(inputStream, file.getSize(), -1)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String url = minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + CommonUtil.getFilePath(filePath);
        return JsonData.buildSuccess(url);
    }
}
