package com.example.blog_froum.service.storage;

import com.example.blog_froum.config.storage.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final StorageProperties properties;

    public LocalFileStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredFile store(MultipartFile file, String relativePath, String fileName, String originalName) throws IOException {
        Path basePath = getUploadBasePath();
        Path filePath = basePath.resolve(relativePath).normalize();
        if (!filePath.startsWith(basePath)) {
            throw new IOException("非法文件路径");
        }

        Files.createDirectories(filePath.getParent());
        file.transferTo(filePath.toFile());

        String fileUrl = trimTrailingSlash(properties.getLocal().getUrlPrefix()) + "/" + normalizePath(relativePath);
        log.info("本地文件保存成功，路径: {}, URL: {}", filePath.toAbsolutePath(), fileUrl);
        return new StoredFile(fileName, fileUrl, normalizePath(relativePath), originalName);
    }

    @Override
    public void delete(String relativePath) throws IOException {
        Path basePath = getUploadBasePath();
        Path filePath = basePath.resolve(relativePath).normalize();
        if (!filePath.startsWith(basePath)) {
            throw new IOException("非法文件路径");
        }
        if (!Files.exists(filePath)) {
            throw new IOException("文件不存在");
        }
        Files.delete(filePath);
    }

    private Path getUploadBasePath() {
        return Paths.get(properties.getLocal().getPath()).toAbsolutePath().normalize();
    }

    private String normalizePath(String value) {
        return value.replace("\\", "/").replaceAll("^/+", "");
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }
}
