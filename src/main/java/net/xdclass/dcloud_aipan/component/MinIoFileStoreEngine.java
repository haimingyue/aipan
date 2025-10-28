package net.xdclass.dcloud_aipan.component;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class MinIoFileStoreEngine implements StoreEngine{

    @Resource
    private AmazonS3Client amazonS3Client;

    @Override
    public boolean bucketExists(String bucketName) {
        return amazonS3Client.doesBucketExistV2(bucketName);
    }

    @Override
    public boolean removeBucket(String bucketName) {
        /**
         * 判断 bucket 是否存在，然后删除bucket
         */
        if (bucketExists(bucketName)) {
            amazonS3Client.deleteBucket(bucketName);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void createBucket(String bucketName) {
        log.info("bucketName:{}", bucketName);
        if (!bucketExists(bucketName)) {
            amazonS3Client.createBucket(bucketName);
        } else {
            log.info("bucket已存在");
        }
    }

    @Override
    public List<Bucket> getAllBucket() {
        return amazonS3Client.listBuckets();
    }

    @Override
    public List<S3ObjectSummary> listObjects(String bucketName) {
        if (bucketExists(bucketName)) {
            return amazonS3Client.listObjects(bucketName).getObjectSummaries();
        } else {
            return null;
        }
    }

    @Override
    public boolean doesObjectExist(String bucketName, String objectKey) {
        if (bucketExists(bucketName)) {
            return amazonS3Client.doesObjectExist(bucketName, objectKey);
        } else {
            return false;
        }
    }

    @Override
    public boolean upload(String bucketName, String objectKey, String localFileName) {
        if (bucketExists(bucketName)) {
            amazonS3Client.putObject(bucketName, objectKey, new File(localFileName));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean upload(String bucketName, String objectKey, MultipartFile file) {
        if (bucketExists(bucketName)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            try {
                amazonS3Client.putObject(bucketName, objectKey, file.getInputStream(), metadata);
                return true;
            } catch (IOException e) {
                log.error("上传文件失败", e);
            }
        }
        return false;
    }

    @Override
    public boolean delete(String bucketName, String objectKey) {
        if (bucketExists(bucketName)) {
            amazonS3Client.deleteObject(bucketName, objectKey);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getDownloadUrl(String bucketName, String objectKey, long timeout, TimeUnit unit) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + unit.toMillis(timeout));
            return amazonS3Client.generatePresignedUrl(bucketName, objectKey, expiration).toString();
        } catch (Exception e) {
            log.error("生成 bucket {} 中对象 {} 的下载 URL 失败: {}", bucketName, e.getMessage(), e);
            return null;
        }
    }

    @Override
    @SneakyThrows
    public void download2Response(String bucketName, String objectKey, HttpServletResponse response) {
        try (S3Object s3Object = amazonS3Client.getObject(bucketName, objectKey)) {
            response.setHeader("Content-Disposition", "attachment;filename=" + objectKey.substring(objectKey.lastIndexOf("/") + 1));
            response.setContentType("application/force-download");
            response.setCharacterEncoding("UTF-8");
            IOUtils.copy(s3Object.getObjectContent(), response.getOutputStream());
        } catch (IOException e) {
            log.error("下载 bucket {} 中对象 {} 失败: {}", bucketName, objectKey, e.getMessage(), e);
        }
    }

    @Override
    public PartListing listMultipart(String bucketName, String objectKey, String uploadId) {
        try {
            ListPartsRequest request = new ListPartsRequest(bucketName, objectKey, uploadId);
            return amazonS3Client.listParts(request);
        } catch (Exception e) {
            log.error("errorMsg={}", e);
            return null;
        }
    }

    @Override
    public InitiateMultipartUploadResult initMultipartUploadTask(String bucketName, String objectKey, ObjectMetadata metadata) {
        try {
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey, metadata);
            return amazonS3Client.initiateMultipartUpload(request);
        } catch (Exception e) {
            log.error("errorMsg={}", e);
            return null;
        }
    }

    @Override
    public URL genePreSignedUrl(String bucketName, String objectKey, HttpMethod httpMethod, Date expiration, Map<String, Object> params) {
        try {
            GeneratePresignedUrlRequest genePreSignedUrlReq =
                    new GeneratePresignedUrlRequest(bucketName, objectKey, httpMethod)
                            .withExpiration(expiration);
            //遍历params作为参数加到genePreSignedUrlReq里面，比如 添加上传ID和分片编号作为请求参数
            //genePreSignedUrlReq.addRequestParameter("uploadId", uploadId);
            //genePreSignedUrlReq.addRequestParameter("partNumber", String.valueOf(i));
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                genePreSignedUrlReq.addRequestParameter(entry.getKey(), String.valueOf(entry.getValue()));
            }
            // 生成并获取预签名URL
            return amazonS3Client.generatePresignedUrl(genePreSignedUrlReq);
        } catch (Exception e) {
            log.error("errorMsg={}", e);
            return null;
        }
    }

    @Override
    public CompleteMultipartUploadResult mergeChunks(String bucketName, String objectKey, String uploadId, List<PartETag> partETags) {
        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(bucketName, objectKey, uploadId, partETags);
        return amazonS3Client.completeMultipartUpload(request);
    }
}
