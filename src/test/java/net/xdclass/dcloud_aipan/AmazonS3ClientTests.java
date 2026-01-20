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
import java.util.*;
import java.util.stream.Collectors;

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

//	==================================================大文件上传==================================================================

	/**
	 * 1. 初始化大文件上传任务，获取 uoloadId
	 * 如果初始化的时候有uoloadId，则说明是续传，不用重新生成uoloadId
	 */

	@Test
	public void testInitMultipartUploadTask() {

		String bucketName = "ai-pan";
		String objectKey = "aa/bb/cc/666.txt";

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType("text/plain");

		// 初始化分片上传请求
		InitiateMultipartUploadRequest initRequest =
				new InitiateMultipartUploadRequest(bucketName, objectKey, metadata);

		// 初始化分片上传任务
		InitiateMultipartUploadResult uploadResult = amazonS3Client.initiateMultipartUpload(initRequest);
		String uploadId = uploadResult.getUploadId();
		log.info("初始化分片上传任务成功，uploadId: {}", uploadId);
	}

	/**
	 * 2. 测试初始化，并生成多个预签名 URL，返回给前端
	 */
	@Test
	public void testGenePreSignedUrls() {

		String bucketName = "ai-pan";
		String objectKey = "aa/bb/cc/666.txt";
		// 分片数量，这里配置 4 个分片
		int chunkCount = 4;
		String uploadId = "NTRiOWIwOTEtN2Q1My00MmZhLTgyYWMtMjM5N2VhNjE4MGU4LjU4NTViMDU2LTdlMzYtNGNlNi1hOWFiLTAwMTBkNmNiZTM2MXgxNzY4MzQ0ODI4MTQ5OTk2MDkz";

		// 存储预签名的地址
		List<String> preSignedUrls = new ArrayList<>(chunkCount);

		// 遍历每个分片，生成预签名地址
		for (int i = 1; i <= chunkCount; i++) {
			// 生成预签名URL，配置过期时间，1小时
			Date expireDate = DateUtil.offsetHour(new Date(), 1);
			// 创建生成预签名URL的请求，并且指定方法为PUT

			GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectKey)
					.withExpiration(expireDate).withMethod(HttpMethod.PUT);

			// 添加上传 ID 和分片编号作为请求参数
			request.addRequestParameter("uploadId", uploadId);
			request.addRequestParameter("partNumber", String.valueOf(i));

			// 请求签名 URL
			URL url = amazonS3Client.generatePresignedUrl(request);
			preSignedUrls.add(url.toString());
			log.info("分片 {} 的预签名URL：{}", i, url);
		}
	}

	/**
	 * 测试合并分片
	 */
	@Test
	public void testMergeChunks() {
		String bucketName = "ai-pan";
		String objectKey = "aa/bb/cc/666.txt";
		// 分片数量，这里配置 4 个分片
		int chunkCount = 4;
		String uploadId = "NTRiOWIwOTEtN2Q1My00MmZhLTgyYWMtMjM5N2VhNjE4MGU4LjU4NTViMDU2LTdlMzYtNGNlNi1hOWFiLTAwMTBkNmNiZTM2MXgxNzY4MzQ0ODI4MTQ5OTk2MDkz";

		// 创建一个列出分片请求对象
		ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, objectKey, uploadId);
		PartListing partListing = amazonS3Client.listParts(listPartsRequest);
		List<PartSummary> partList = partListing.getParts();

		// 检查分片和预期的是否一致
		if (partList.size() != chunkCount) {
			// 已经上传的分片和记录中的不一样，不能合并
			throw new RuntimeException("分片数量不一致");
		}

		// 创建完成分片上传请求对象，进行合并
		CompleteMultipartUploadRequest completeMultipartUploadRequest
				= new CompleteMultipartUploadRequest()
				.withBucketName(bucketName)
				.withKey(objectKey)
				.withUploadId(uploadId)
				.withPartETags(
						partList.stream()
								.map(partSummary -> new PartETag(partSummary.getPartNumber(), partSummary.getETag()))
								 .collect(Collectors.toList()));

		CompleteMultipartUploadResult result = amazonS3Client.completeMultipartUpload(completeMultipartUploadRequest);
		log.info("合并分片成功，返回结果：{}", result);
	}

	/**
	 * 上传进度验证
	 */
	@Test
	public void testGetUploadProgress() {
		String bucketName = "ai-pan";
		String objectKey = "aa/bb/cc/666.txt";
		// 分片数量，这里配置 4 个分片
		int chunkCount = 4;
		String uploadId = "NTRiOWIwOTEtN2Q1My00MmZhLTgyYWMtMjM5N2VhNjE4MGU4LjU4NTViMDU2LTdlMzYtNGNlNi1hOWFiLTAwMTBkNmNiZTM2MXgxNzY4MzQ0ODI4MTQ5OTk2MDkz";

		// 检查对应的桶里面是否存在对应的对象
		boolean doesObjectExist = amazonS3Client.doesObjectExist(bucketName, objectKey);

		if (!doesObjectExist) {
			ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, objectKey, uploadId);
			PartListing partListing = amazonS3Client.listParts(listPartsRequest);
			List<PartSummary> partList = partListing.getParts();

			// 创建一个结果，用于存储上传状态和分片列表
			Map<String, Object> result = new HashMap<>(2);
			result.put("finished", false);
			result.put("existPartList", partList);

			// 前端可以通过finished判断是否要调用 merge
			log.info("上传进度：{}", result);

			// 遍历每个分片的信息
			for (PartSummary partSummary : partList) {
				System.out.println(
						"分片编号：" + partSummary.getPartNumber() +
								"，ETag：" + partSummary.getETag() +
								"，大小：" + partSummary.getSize() +
								"，上传时间：" + partSummary.getLastModified()
				);
			}

		}
	}
}
