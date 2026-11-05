package org.dromara.apply.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 小程序端 Web 配置
 *
 * @author want-do-apply
 */
@Configuration
@RequiredArgsConstructor
public class AppWebConfig implements WebMvcConfigurer {

    @Value("${apply.upload-dir:upload/app}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AppAuthInterceptor())
            .addPathPatterns("/app/**")
            .excludePathPatterns(
                "/app/auth/login",
                "/app/auth/config"
            );
    }

    /**
     * 上传文件对外访问
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/app-file/**").addResourceLocations(location);
    }
}
