package net.xdclass.dcloud_aipan;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.controller.req.FileChunkInitTaskReq;
import net.xdclass.dcloud_aipan.controller.req.FileChunkMergeReq;
import net.xdclass.dcloud_aipan.dto.FileChunkDTO;
import net.xdclass.dcloud_aipan.service.FileChunkService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static cn.hutool.core.lang.Console.log;

@SpringBootTest
@Slf4j
public class FileChunkUploadTest {

    @Autowired
    private FileChunkService fileChunkService;

    private Long accountId = 2005080216221016066L;

    private String identifier = "2021-08-02-16-22-10-26-2005080216221016066";

    // 存储分片文件的路径
    private final List<String> chunkFilePaths = new ArrayList<>();

    /**
     * 存储分片上传的临时签名地址
     */
    private final List<String> chunkUploadUrls = new ArrayList<>();

    private String uploadId;

    private final long chunkSize = 1024 * 1024 * 5;

    /**
     * 模拟文件分片生成小的chunk文件
     */
    @Test
    public void testCreateFileChunk() {
        // 将文件分片并存储
        String filePath = "/Users/simoonqian/Desktop/third/test.sz";

        File file = new File(filePath);
        long fileSize = file.length();

        // 计算分片数量
        int chunkNum = (int) Math.ceil(fileSize * 1.0 / chunkSize);
        FileChunkUploadTest.log.info("分片数量：{}", chunkNum);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int)chunkSize];
            for (int i = 0; i < chunkNum; i++) {
//                int read = fis.read(buffer);
                String chunkFileName = filePath + ".part" + (i+1);
                try (FileOutputStream fos = new FileOutputStream(chunkFileName)) {
                    int bytesRead = fis.read(buffer);
                    fos.write(buffer, 0, bytesRead);
                    FileChunkUploadTest.log.info("分片文件：{}，大小：{}", chunkFileName, bytesRead);
                    fos.flush();
                    chunkFilePaths.add(chunkFileName);
                    FileChunkUploadTest.log.info("分片存储路径: {}", chunkFilePaths);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 创建分片上传任务
        testInitFileChunkTask();
    }

    private void testInitFileChunkTask() {
        FileChunkInitTaskReq req = new FileChunkInitTaskReq();
        req.setAccountId(accountId)
                .setIdentifier(identifier)
                .setFilename("test.sz")
                .setTotalSize((long)25343569)
                .setChunkSize(chunkSize);
        FileChunkDTO fileChunkDTO = fileChunkService.initFileChunkTask(req);
        uploadId = fileChunkDTO.getUploadId();

        // 获取分片上传地址
        testGetFileChunkUploadUrl();
    }

    // 2. 获取分片临时上传地址
    private void testGetFileChunkUploadUrl() {
        for (int i = 1; i <= chunkFilePaths.size(); i++) {
            String uploadFileUrl = fileChunkService.genPreSignUploadUrl(accountId, identifier, i);
            log("分片上传地址：{}", uploadFileUrl);
            chunkUploadUrls.add(uploadFileUrl);
        }

        // 上传分片文件，模拟前端上传
        uploadChunk();
    }

    // 3- 模拟前端上传
    @SneakyThrows
    private void uploadChunk() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        for (int i = 0; i < chunkUploadUrls.size(); i++) {
            // 使用 put 方法上传
            String uploadUrl = chunkUploadUrls.get(i);
            HttpPut httpPut = new HttpPut(uploadUrl);
            httpPut.setHeader("Content-Type", "application/octet-stream");
            File chunkFile = new File(chunkFilePaths.get(i));
            httpPut.setEntity(new FileEntity(chunkFile));
            CloseableHttpResponse response = httpClient.execute(httpPut);
            log("分片上传结果：{}", response.getStatusLine());
            httpPut.releaseConnection();
        }
        log("分片上传完成");
    }


    @Test
    public void testMergeFileChunk() {
        FileChunkMergeReq req = new FileChunkMergeReq();
        req.setAccountId(accountId);
        req.setIdentifier(identifier);
        req.setParentId(2005080217001156610L);
        fileChunkService.mergeFileChunk(req);
    }

    @Test
    public void testChunkUploadProgress() {
        FileChunkDTO fileChunkDTO = fileChunkService.listFileChunk(accountId, identifier);
        log("分片上传进度：{}", fileChunkDTO);
    }
}
