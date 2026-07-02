package com.example.blog_froum.config.storage;

import com.example.blog_froum.service.storage.FileStorageService;
import com.example.blog_froum.service.storage.LocalFileStorageService;
import com.example.blog_froum.service.storage.R2FileStorageService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    @Bean
    public FileStorageService fileStorageService(StorageProperties properties) {
        if ("r2".equalsIgnoreCase(properties.getType())) {
            return new R2FileStorageService(properties);
        }
        return new LocalFileStorageService(properties);
    }
}
