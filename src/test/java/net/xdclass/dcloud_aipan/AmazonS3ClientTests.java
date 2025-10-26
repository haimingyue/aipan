package net.xdclass.dcloud_aipan;

import cn.hutool.core.date.DateUtil;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.Optional;

@SpringBootTest
@Slf4j
class AmazonS3ClientTests {

	@Autowired
	private AmazonS3Client amazonS3Client;


	/**
	 * 判断 Bucket 是否存在
	 */
	@Test
	public void testBucketExists() {
		boolean exists = amazonS3Client.doesBucketExistV2("ai-pan");
		log.info("Bucket 存在吗？{}", exists);
	}

	/**
	 * 创建 Bucket
	 */
	@Test
	public void testCreateBucket() {
		String bucketName = "ai-pan1";
		Bucket bucket = amazonS3Client.createBucket(bucketName);
		log.info("创建 Bucket {} 成功", bucket);
	}

	/**
	 * 删除 bucket
	 */
	@Test
	public void testDeleteBucket() {
		String bucketName = "ai-pan1";
		amazonS3Client.deleteBucket(bucketName);
		log.info("删除 Bucket {} 成功", bucketName);
	}

	/**
	 * 列出所有 Bucket
	 */
	@Test
	public void testListBuckets() {
		for (Bucket bucket : amazonS3Client.listBuckets()) {
			log.info("Bucket {}", bucket.getName());
		}
	}
	/**
	 * 根据 bucket 的名称获取详情
	 */
	@Test
	public void testGetBucket() {
		String bucketName = "ai-pan";
		Optional<Bucket> first = amazonS3Client.listBuckets().stream()
				.filter(bucket -> bucket.getName().equals(bucketName)).findFirst();
		if (first.isPresent()) {
			log.info("Bucket {} 详情：{}", bucketName, first.get());
		} else {
			log.info("Bucket {} 不存在", bucketName);
		}
	}

	// 文件操作
	/**
	 * 上传单个文件，直接写入文本
	 */
	@Test
	public void testUploadFile() {
		PutObjectResult helloWorld = amazonS3Client.putObject("ai-pan", "test.txt", "hello world");
		log.info("上传文件成功，返回结果：{}", helloWorld);
	}

	/**
	 * 上传单个文件，采用本地文件路径
	 */
	@Test
	public void testUploadFileByPath() {
		PutObjectResult putObject = amazonS3Client.putObject("ai-pan", "/aa/bb/111.png",
				new File("/Users/simoonqian/Desktop/WechatIMG514.jpg"));
		log.info("上传文件成功，返回结果：{}", putObject);
	}

	/**
	 * 上传单个文件，采用 InputStream，带上文件元数据
	 */
	@Test
	public void testUploadFileByInputStream() {
		try (FileInputStream fileInputStream = new FileInputStream("/Users/simoonqian/Desktop/WechatIMG514.jpg")){
//			amazonS3Client.putObject("ai-pan", "111.png", fileInputStream, null);
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentType("image/jpeg");
			PutObjectResult putObjectResult = amazonS3Client.putObject("ai-pan", "111.png", fileInputStream, metadata);
			log.info("上传文件成功，返回结果：{}", putObjectResult.getMetadata());
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	/**
	 * 获取文件
	 */
	@Test
	public void testGetFile() {
        try (FileOutputStream fileOutputStream = new FileOutputStream("/Users/simoonqian/Desktop/copy1.jpg")){
			S3Object object = amazonS3Client.getObject("ai-pan", "111.png");
			object.getObjectContent().transferTo(fileOutputStream);
		} catch (SdkClientException | IOException e) {
            throw new RuntimeException(e);
        }
    }
	/**
	 * 删除文件
	 */
	@Test
	public void testDeleteFile() {
		amazonS3Client.deleteObject("ai-pan", "111.png");
		log.info("删除文件成功");
	}

	/**
	 * Generate Access Address
	 */
	@Test
	public void testGeneratePresignedUrl() {
		// 预签名url过期时间(ms)
		long PRE_SIGN_URL_EXPIRE = 60 * 10 * 1000L;
		// 计算预签名url的过期日期
		Date expireDate = DateUtil.offsetMillisecond(new Date(), (int) PRE_SIGN_URL_EXPIRE);
		// 创建生成预签名url的请求，并设置过期时间和HTTP方法, withMethod是生成的URL访问方式,是权限控制的一种方式
		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest("ai-pan", "aa/bb/111.png")
				.withExpiration(expireDate).withMethod(HttpMethod.GET);


		// 生成预签名url
		URL preSignedUrl = amazonS3Client.generatePresignedUrl(request);

		// 输出预签名url
		System.out.println(preSignedUrl.toString());
	}
}
