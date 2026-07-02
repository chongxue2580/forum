package com.example.blog_froum.service.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    StoredFile store(MultipartFile file, String relativePath, String fileName, String originalName) throws IOException;

    void delete(String relativePath) throws IOException;
}
