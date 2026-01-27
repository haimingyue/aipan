package net.xdclass.dcloud_aipan.service.impl;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Value("${ai.key}")
    private String apiKey;

    @Override
    public GenerationResult callWithMessage(String input) throws Exception {

        // 初始化大模型生成器
        Generation gen = new Generation();

        // 构建系统角色消息
        Message systemMessage = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一个智能助手，名字叫惊风")
                .build();

        // 构建用户消息
        Message userMessage = Message.builder()
                .role(Role.USER.getValue())
                .content(input)
                .build();

        // 构建请求参数，配置模型的其他关键参数和对话上下文
        GenerationParam generationParam = GenerationParam.builder()
                .model(Generation.Models.QWEN_TURBO)
                .messages(Arrays.asList(systemMessage, userMessage))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .temperature(0.7f)
                .apiKey(apiKey)
                .build();


        return gen .call(generationParam);
    }
}
