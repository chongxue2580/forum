package com.example.blog_froum.dto.upload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadInfoResponse {
    private String currentDir;
    private String configuredUploadPath;
    private String urlPrefix;
    private String actualUploadPath;
    private boolean uploadDirExists;
    private boolean uploadDirCanWrite;
    private boolean imagesDirExists;
    private boolean imagesDirCanWrite;
    private boolean canCreateDirectories;
    private String testPathCreated;
    private String createDirectoryError;
}
