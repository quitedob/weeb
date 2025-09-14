package com.web.runner;


import com.web.annotation.UrlFree;
import com.web.annotation.UrlResource;
import com.web.util.UrlPermitUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.annotation.Resource;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: dwh
 **/
@Component
public class UrlPassRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(UrlPassRunner.class);

    @Resource
    private UrlPermitUtil urlPermitUtil;

    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    public void run(ApplicationArguments args) {
        Map<RequestMappingInfo, HandlerMethod> methodMap = requestMappingHandlerMapping.getHandlerMethods();
        List<String> urlList = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : methodMap.entrySet()) {
            RequestMappingInfo requestMappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();
            Annotation[] annotations = handlerMethod.getMethod().getAnnotations();
            for (Annotation annotation : annotations) {
                // 免验证url
                if (annotation.annotationType().equals(UrlFree.class)) {
                    //获取请求路径
                    Set<String> directPaths = requestMappingInfo.getPatternValues();
                    for (String url : directPaths) {
                        urlList.add(url.replaceAll("\\{[^\\}]+\\}", "**"));
                    }
                }
                // 免验证url
                if (annotation.annotationType().equals(UrlResource.class)) {
                    UrlResource urlResource = (UrlResource) annotation;
                    String value = urlResource.value();
                    //获取请求路径
                    Set<String> directPaths = requestMappingInfo.getPatternValues();
                    for (String url : directPaths) {
                        urlPermitUtil.addRoleUrl(value, url);
                    }
                }
            }
        }
        urlPermitUtil.addUrls(urlList);
        logger.info("-----not verify that the url is successfully loaded-----");
    }
}


