package net.xdclass.dcloud_aipan;

import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.controller.req.AccountLoginReq;
import net.xdclass.dcloud_aipan.controller.req.AccountRegisterReq;
import net.xdclass.dcloud_aipan.dto.AccountDTO;
import net.xdclass.dcloud_aipan.service.AccountService;
import net.xdclass.dcloud_aipan.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class AccountServiceTest {
    @Autowired
    private AccountService accountService;

    // 测试注册方法
    @Test
    public void testRegister() {
        AccountRegisterReq req = AccountRegisterReq.builder().phone("123").password("123").username("老王").avatarUrl("xdclass.net").build();
        accountService.register(req);
    }

    //登录方法测试
    @Test
    public void testLogin() {
        AccountLoginReq req = AccountLoginReq.builder().phone("123").password("123").build();
        AccountDTO accountDTO = accountService.login(req);
        String token = JwtUtil.geneLoginJWT(accountDTO);
        log.info("token:{}", token);
        System.out.println(accountDTO);
    }

    @Test
    public void testQueryDetail() {
        AccountDTO accountDTO = accountService.queryDetail(2003600138601623554L);
        System.out.println(accountDTO);
    }
}
