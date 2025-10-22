package com.web.aop;

import com.web.annotation.AdminLog;
import com.web.model.SystemLog;
import com.web.security.SecurityUtils;
import com.web.service.LogService;
import com.web.util.UserIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class AdminLogAspect {

    @Autowired
    private LogService logService;

    @AfterReturning(pointcut = "@annotation(com.web.annotation.AdminLog)")
    public void logAdminAction(JoinPoint joinPoint) {
        try {
            SystemLog systemLog = new SystemLog();

            // 1. 获取操作员ID
            Long operatorId = SecurityUtils.getCurrentUserId();
            systemLog.setOperatorId(operatorId);

            // 2. 获取IP地址
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            systemLog.setIpAddress(UserIpUtils.getUserIp(request));

            // 3. 获取注解中的操作描述
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            AdminLog adminLog = method.getAnnotation(AdminLog.class);
            systemLog.setAction(adminLog.action());

            // 4. 获取操作详情 (方法参数)
            Object[] args = joinPoint.getArgs();
            String details = Arrays.stream(args)
                                   .map(Object::toString)
                                   .collect(Collectors.joining(", "));
            systemLog.setDetails("Parameters: [" + details + "]");

            // 5. 记录日志
            logService.recordLog(systemLog);

        } catch (Exception e) {
            log.error("记录管理员日志时出错", e);
        }
    }
}
