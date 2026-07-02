package com.example.blog_froum.service.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoredFile {
    private String fileName;
    private String url;
    private String relativePath;
    private String originalName;
}
