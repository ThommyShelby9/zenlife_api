package com.api.expo.WebMvcConfigurer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

@Configuration
public class WebConfig implements WebMvcConfigurer {
/* 
    @Override
    public void addCorsMappings(@SuppressWarnings("null") CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

    } */

     @Override
    @SuppressWarnings({ "deprecation", "null", "unchecked" })
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        List<MediaType> mediaTypes = java.util.Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON_UTF8,
            new MediaType("application", "json", StandardCharsets.UTF_8)
        );
        jsonConverter.setSupportedMediaTypes(mediaTypes);
        converters.add(jsonConverter);
    }

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/view/profile_pictures/**")
                .addResourceLocations("file:" + uploadDir + "/profile_pictures/");
    }
}
