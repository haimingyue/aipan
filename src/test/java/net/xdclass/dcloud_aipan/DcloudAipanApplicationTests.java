package net.xdclass.dcloud_aipan;

import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import lombok.SneakyThrows;
import net.xdclass.dcloud_aipan.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class  DcloudAipanApplicationTests {

	@Autowired
	private ChatService chatService;

	@Test
	@SneakyThrows
	public void testAI() {
		GenerationResult generationResult = chatService.callWithMessage("你是谁？帮我写一个 Java 的冒泡排序");
		GenerationOutput output = generationResult.getOutput();
		String content = output.getChoices().get(0).getMessage().getContent();
		System.out.println(content);
		System.exit(0);
	}

}
