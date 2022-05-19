package com.hmdp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Losca
 * @date 2022/5/18 21:43
 */
@Configuration
public class LoginInterceptorConfig implements WebMvcConfigurer {
    @Autowired
    LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration registration = registry.addInterceptor(loginInterceptor);
        registration.addPathPatterns("/**").excludePathPatterns("/shop/**",
                "/voucher/**",
                "/shop-type/**",
                "/upload/**",
                "/blog/hot",
                "/user/code",
                "/user/login");
    }
}
