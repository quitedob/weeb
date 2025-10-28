package com.web.Config;

// import com.web.interceptor.PermissionInterceptor; // 权限系统已禁用
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * WebMvc 配置类，用于扩展 Spring MVC 的功能。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${weeb.upload.base-path:uploads}")
    private String baseUploadPath;

    // @Autowired
    // private PermissionInterceptor permissionInterceptor; // 权限系统已禁用

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 添加自定义参数解析器
        resolvers.add(new UserInfoArgumentResolver());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置本地文件上传目录的静态资源映射
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + baseUploadPath + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 权限验证拦截器已禁用（权限系统关闭）
        // registry.addInterceptor(permissionInterceptor)
        //         .addPathPatterns("/api/**")  // 拦截所有API请求
        //         .excludePathPatterns(
        //                 "/api/auth/**",      // 排除认证相关接口
        //                 "/api/public/**",    // 排除公开接口
        //                 "/api/health",       // 排除健康检查
        //                 "/api/actuator/**"   // 排除监控端点
        //         );
    }
}
