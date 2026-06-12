package com.example.blog_froum.config;

import com.example.blog_froum.interceptor.JwtTokenAdminInterceptor;
import com.example.blog_froum.interceptor.JwtTokenUserInterceptor;
import com.example.blog_froum.utils.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;
import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@EnableOpenApi
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    /**
     * 配置静态资源映射
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上传文件的静态资源映射
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");

        // 配置Swagger UI资源
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
    }

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        
        // 管理员拦截器
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");

        // 用户拦截器 - 拦截需要认证的接口，内部根据HTTP方法判断
        registry.addInterceptor(jwtTokenUserInterceptor)
                // 拦截用户相关接口
                .addPathPatterns("/api/user/**")
                // 拦截文章相关接口（内部会根据HTTP方法判断是否需要认证）
                .addPathPatterns("/api/articles/**")
                // 拦截评论相关接口（内部会根据HTTP方法判断是否需要认证）
                .addPathPatterns("/api/comments/**")
                // 拦截问答相关接口（内部会根据HTTP方法判断是否需要认证）
                .addPathPatterns("/api/questions/**")
                // 拦截通知相关接口
                .addPathPatterns("/api/notifications")
                .addPathPatterns("/api/notifications/*")
                // 拦截文件上传接口
                .addPathPatterns("/api/upload/*")
                // 拦截关注相关接口
                .addPathPatterns("/api/follows/**")
                // 拦截点赞相关接口
                .addPathPatterns("/api/likes/**")
                // 排除不需要认证的接口
                .excludePathPatterns("/api/user/login")
                .excludePathPatterns("/api/user/register")
                .excludePathPatterns("/api/user/profile/login")
                .excludePathPatterns("/api/user/profile/register")
                .excludePathPatterns("/api/user/check-username")
                .excludePathPatterns("/api/user/check-email");

        // 配置管理员拦截器
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");
    }

    /**
     * 通过knife4j生成管理端接口文档
     * @return
     */
    @Bean
    public Docket adminDocket() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("博客论坛系统接口文档")
                .version("2.0")
                .description("博客论坛系统管理端接口文档")
                .contact(new Contact("开发团队", "", "developer@example.com"))
                .build();
        
        Docket docket = new Docket(DocumentationType.OAS_30)
                .groupName("管理端接口文档")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.example.blog_froum.controller.admin"))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
        return docket;
    }

    /**
     * 通过knife4j生成用户端接口文档
     * @return
     */
    @Bean
    public Docket userDocket() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("博客论坛系统接口文档")
                .version("2.0")
                .description("博客论坛系统用户端接口文档")
                .contact(new Contact("开发团队", "", "developer@example.com"))
                .build();
        
        Docket docket = new Docket(DocumentationType.OAS_30)
                .groupName("用户端接口文档")
                .apiInfo(apiInfo)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.example.blog_froum.controller.user"))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(securitySchemes())
                .securityContexts(securityContexts());
        return docket;
    }

    /**
     * 安全模式，这里指定token通过Authorization头请求头传递
     */
    private List<SecurityScheme> securitySchemes() {
        ApiKey apiKey = new ApiKey("JWT", "Authorization", "header");
        return Collections.singletonList(apiKey);
    }

    /**
     * 安全上下文
     */
    private List<SecurityContext> securityContexts() {
        SecurityContext securityContext = SecurityContext.builder()
                .securityReferences(defaultAuth())
                .operationSelector(o -> o.requestMappingPattern().matches("/api/.*"))
                .build();
        return Collections.singletonList(securityContext);
    }

    /**
     * 默认的安全上下文
     */
    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Collections.singletonList(new SecurityReference("JWT", authorizationScopes));
    }


    /**
     * 扩展spring mvc框架的消息转换器
     * @param converters
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        //需要为消息转化器设置一个对象转化器，对象转化器可以将java对象序列化为json数据
        converter.setObjectMapper(new JacksonObjectMapper());
        //将自己的消息转化器加入容器中
        converters.add(0, converter);
    }


}
