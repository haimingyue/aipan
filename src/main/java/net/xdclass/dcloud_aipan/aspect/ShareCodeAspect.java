package net.xdclass.dcloud_aipan.aspect;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.dcloud_aipan.annotation.ShareCodeCheck;
import net.xdclass.dcloud_aipan.enums.BizCodeEnum;
import net.xdclass.dcloud_aipan.exception.BizException;
import net.xdclass.dcloud_aipan.util.JsonData;
import net.xdclass.dcloud_aipan.util.JwtUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class ShareCodeAspect {

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void set(Long shareId) {
        threadLocal.set(shareId);
    }

    public static Long get() {
        return threadLocal.get();
    }

    @Pointcut("@annotation(shareCodeCheck)")
    public void pointCutShareCheckCode(ShareCodeCheck shareCodeCheck ) {

    }

    /**
     * 配置环绕通知
     */
    @Around("pointCutShareCheckCode(shareCodeCheck)")
    public Object around(ProceedingJoinPoint joinPoint,ShareCodeCheck shareCodeCheck) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.getRequestAttributes())).getRequest();
        String shareToken = request.getParameter("share-token");

        if (StringUtils.isBlank(shareToken)) {
            throw new BizException(BizCodeEnum.SHARE_CODE_ILLEGAL);
        }
        Claims claims = JwtUtil.checkShareJWT(shareToken);
        if (claims == null) {
            log.error("share-token 解析失败");
            return JsonData.buildResult(BizCodeEnum.SHARE_CODE_ILLEGAL);
        }
        Long shareId = Long.valueOf(claims.get(JwtUtil.CLAIM_SHARE_KEY) + "");
        set(shareId);
        log.info("环绕通知执行前:{}",shareId);
        Object obj = joinPoint.proceed();
        log.info("环绕通知执行后:{}",shareId);
        return obj;

    }

}
