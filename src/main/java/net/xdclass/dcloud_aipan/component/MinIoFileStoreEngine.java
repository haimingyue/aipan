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
import java.util.Objects;


@Component
@Slf4j
public class MinIoFileStoreEngine implements StoreEngine{

    @Resource
    private AmazonS3Client amazonS3Client;

    /**
     * 检查bucket是否存在
     * @param bucketName bucket名称
     * @return 存在返回true，否则返回false
     */
    @Override
    public boolean bucketExists(String bucketName) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            return amazonS3Client.doesBucketExistV2(bucketName);
        } catch (Exception e) {
            log.error("检查bucket {} 是否存在失败: {}", bucketName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 删除bucket
     * @param bucketName bucket名称
     * @return 删除成功返回true，否则返回false
     */
    @Override
    public boolean removeBucket(String bucketName) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            /**
             * 判断 bucket 是否存在，然后删除bucket
             */
            if (bucketExists(bucketName)) {
                amazonS3Client.deleteBucket(bucketName);
                log.info("成功删除bucket: {}", bucketName);
                return true;
            } else {
                log.warn("尝试删除不存在的bucket: {}", bucketName);
                return false;
            }
        } catch (Exception e) {
            log.error("删除bucket {} 失败: {}", bucketName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建bucket
     * @param bucketName bucket名称
     */
    @Override
    public void createBucket(String bucketName) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            log.info("准备创建bucket: {}", bucketName);
            if (!bucketExists(bucketName)) {
                amazonS3Client.createBucket(bucketName);
                log.info("成功创建bucket: {}", bucketName);
            } else {
                log.info("bucket已存在: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("创建bucket {} 失败: {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("创建bucket失败: " + bucketName, e);
        }
    }

    /**
     * 获取所有bucket列表
     * @return bucket列表
     */
    @Override
    public List<Bucket> getAllBucket() {
        try {
            return amazonS3Client.listBuckets();
        } catch (Exception e) {
            log.error("获取bucket列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取bucket列表失败", e);
        }
    }

    /**
     * 列出bucket中的所有对象
     * @param bucketName bucket名称
     * @return 对象列表
     */
    @Override
    public List<S3ObjectSummary> listObjects(String bucketName) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            if (bucketExists(bucketName)) {
                return amazonS3Client.listObjects(bucketName).getObjectSummaries();
            } else {
                log.warn("bucket {} 不存在", bucketName);
                return null;
            }
        } catch (Exception e) {
            log.error("列出bucket {} 中的对象失败: {}", bucketName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查对象是否存在
     * @param bucketName bucket名称
     * @param objectKey 对象键
     * @return 存在返回true，否则返回false
     */
    @Override
    public boolean doesObjectExist(String bucketName, String objectKey) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            Objects.requireNonNull(objectKey, "objectKey不能为空");
            if (bucketExists(bucketName)) {
                return amazonS3Client.doesObjectExist(bucketName, objectKey);
            } else {
                log.warn("bucket {} 不存在", bucketName);
                return false;
            }
        } catch (Exception e) {
            log.error("检查对象 {} 在bucket {} 中是否存在失败: {}", objectKey, bucketName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 上传本地文件到S3
     * @param bucketName bucket名称
     * @param objectKey 对象键
     * @param localFileName 本地文件路径
     * @return 上传成功返回true，否则返回false
     */
    @Override
    public boolean upload(String bucketName, String objectKey, String localFileName) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            Objects.requireNonNull(objectKey, "objectKey不能为空");
            Objects.requireNonNull(localFileName, "localFileName不能为空");

            if (bucketExists(bucketName)) {
                amazonS3Client.putObject(bucketName, objectKey, new File(localFileName));
                log.info("成功上传文件 {} 到 bucket {}，对象键为 {}", localFileName, bucketName, objectKey);
                return true;
            } else {
                log.warn("bucket {} 不存在，无法上传文件 {}", bucketName, localFileName);
                return false;
            }
        } catch (Exception e) {
            log.error("上传文件 {} 到 bucket {} 失败: {}", localFileName, bucketName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 上传MultipartFile到S3
     * @param bucketName bucket名称
     * @param objectKey 对象键
     * @param file MultipartFile对象
     * @return 上传成功返回true，否则返回false
     */
    @Override
    public boolean upload(String bucketName, String objectKey, MultipartFile file) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            Objects.requireNonNull(objectKey, "objectKey不能为空");
            Objects.requireNonNull(file, "file不能为空");

            if (bucketExists(bucketName)) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(file.getContentType());
                metadata.setContentLength(file.getSize());
                amazonS3Client.putObject(bucketName, objectKey, file.getInputStream(), metadata);
                log.info("成功上传MultipartFile到 bucket {}，对象键为 {}", bucketName, objectKey);
                return true;
            } else {
                log.warn("bucket {} 不存在，无法上传文件", bucketName);
                return false;
            }
        } catch (IOException e) {
            log.error("上传MultipartFile到 bucket {} 失败: {}", bucketName, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("上传MultipartFile到 bucket {} 时发生未知错误: {}", bucketName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 删除S3对象
     * @param bucketName bucket名称
     * @param objectKey 对象键
     * @return 删除成功返回true，否则返回false
     */
    @Override
    public boolean delete(String bucketName, String objectKey) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            Objects.requireNonNull(objectKey, "objectKey不能为空");

            if (bucketExists(bucketName)) {
                amazonS3Client.deleteObject(bucketName, objectKey);
                log.info("成功删除bucket {} 中的对象 {}", bucketName, objectKey);
                return true;
            } else {
                log.warn("bucket {} 不存在，无法删除对象 {}", bucketName, objectKey);
                return false;
            }
        } catch (Exception e) {
            log.error("删除bucket {} 中的对象 {} 失败: {}", bucketName, objectKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成预签名下载URL
     * @param bucketName bucket名称
     * @param objectKey 对象键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 预签名URL
     */
    @Override
    public String getDownloadUrl(String bucketName, String objectKey, long timeout, TimeUnit unit) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            Objects.requireNonNull(objectKey, "objectKey不能为空");
            Objects.requireNonNull(unit, "unit不能为空");

            Date expiration = new Date(System.currentTimeMillis() + unit.toMillis(timeout));
            URL url = amazonS3Client.generatePresignedUrl(bucketName, objectKey, expiration);
            log.info("成功生成bucket {} 中对象 {} 的下载URL", bucketName, objectKey);
            return url.toString();
        } catch (Exception e) {
            log.error("生成 bucket {} 中对象 {} 的下载 URL 失败: {}", bucketName, objectKey, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 下载文件到HttpServletResponse
     * @param bucketName bucket名称
     * @param objectKey 对象键
     * @param response HttpServletResponse对象
     */
    @Override
    @SneakyThrows
    public void download2Response(String bucketName, String objectKey, HttpServletResponse response) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            Objects.requireNonNull(objectKey, "objectKey不能为空");
            Objects.requireNonNull(response, "response不能为空");

            try (S3Object s3Object = amazonS3Client.getObject(bucketName, objectKey)) {
                response.setHeader("Content-Disposition", "attachment;filename=" + objectKey.substring(objectKey.lastIndexOf("/") + 1));
                response.setContentType("application/force-download");
                response.setCharacterEncoding("UTF-8");
                IOUtils.copy(s3Object.getObjectContent(), response.getOutputStream());
                log.info("成功下载bucket {} 中的对象 {}", bucketName, objectKey);
            }
        } catch (IOException e) {
            log.error("下载 bucket {} 中对象 {} 失败: {}", bucketName, objectKey, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("下载 bucket {} 中对象 {} 时发生未知错误: {}", bucketName, objectKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 列出分片上传任务
     * @param bucketName bucket名称
     * @param objectKey 对象键
     * @param uploadId 上传ID
     * @return 分片列表
     */
    @Override
    public PartListing listMultipart(String bucketName, String objectKey, String uploadId) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            Objects.requireNonNull(objectKey, "objectKey不能为空");
            Objects.requireNonNull(uploadId, "uploadId不能为空");

            ListPartsRequest request = new ListPartsRequest(bucketName, objectKey, uploadId);
            PartListing result = amazonS3Client.listParts(request);
            log.info("成功列出bucket {} 中对象 {} 的分片上传任务 {}", bucketName, objectKey, uploadId);
            return result;
        } catch (Exception e) {
            log.error("列出bucket {} 中对象 {} 的分片上传任务 {} 失败: {}", bucketName, objectKey, uploadId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 初始化分片上传任务
     * @param bucketName bucket名称
     * @param objectKey 对象键
     * @param metadata 对象元数据
     * @return 初始化结果
     */
    @Override
    public InitiateMultipartUploadResult initMultipartUploadTask(String bucketName, String objectKey, ObjectMetadata metadata) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            Objects.requireNonNull(objectKey, "objectKey不能为空");

            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey, metadata);
            InitiateMultipartUploadResult result = amazonS3Client.initiateMultipartUpload(request);
            log.info("成功初始化bucket {} 中对象 {} 的分片上传任务，uploadId: {}", bucketName, objectKey, result.getUploadId());
            return result;
        } catch (Exception e) {
            log.error("初始化bucket {} 中对象 {} 的分片上传任务失败: {}", bucketName, objectKey, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成预签名URL
     * @param bucketName bucket名称
     * @param objectKey 对象键
     * @param httpMethod HTTP方法
     * @param expiration 过期时间
     * @param params 请求参数
     * @return 预签名URL
     */
    @Override
    public URL genePreSignedUrl(String bucketName, String objectKey, HttpMethod httpMethod, Date expiration, Map<String, Object> params) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            Objects.requireNonNull(objectKey, "objectKey不能为空");
            Objects.requireNonNull(httpMethod, "httpMethod不能为空");
            Objects.requireNonNull(expiration, "expiration不能为空");

            GeneratePresignedUrlRequest genePreSignedUrlReq =
                    new GeneratePresignedUrlRequest(bucketName, objectKey, httpMethod)
                            .withExpiration(expiration);
            //遍历params作为参数加到genePreSignedUrlReq里面，比如 添加上传ID和分片编号作为请求参数
            //genePreSignedUrlReq.addRequestParameter("uploadId", uploadId);
            //genePreSignedUrlReq.addRequestParameter("partNumber", String.valueOf(i));
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    genePreSignedUrlReq.addRequestParameter(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            // 生成并获取预签名URL
            URL url = amazonS3Client.generatePresignedUrl(genePreSignedUrlReq);
            log.info("成功生成bucket {} 中对象 {} 的预签名URL", bucketName, objectKey);
            return url;
        } catch (Exception e) {
            log.error("生成bucket {} 中对象 {} 的预签名URL失败: {}", bucketName, objectKey, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 合并分片
     * @param bucketName bucket名称
     * @param objectKey 对象键
     * @param uploadId 上传ID
     * @param partETags 分片标签列表
     * @return 合并结果
     */
    @Override
    public CompleteMultipartUploadResult mergeChunks(String bucketName, String objectKey, String uploadId, List<PartETag> partETags) {
        try {
            Objects.requireNonNull(bucketName, "bucketName不能为空");
            Objects.requireNonNull(objectKey, "objectKey不能为空");
            Objects.requireNonNull(uploadId, "uploadId不能为空");
            Objects.requireNonNull(partETags, "partETags不能为空");

            CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(bucketName, objectKey, uploadId, partETags);
            CompleteMultipartUploadResult result = amazonS3Client.completeMultipartUpload(request);
            log.info("成功合并bucket {} 中对象 {} 的分片，uploadId: {}", bucketName, objectKey, uploadId);
            return result;
        } catch (Exception e) {
            log.error("合并bucket {} 中对象 {} 的分片失败，uploadId: {}: {}", bucketName, objectKey, uploadId, e.getMessage(), e);
            throw new RuntimeException("合并分片失败", e);
        }
    }
}
