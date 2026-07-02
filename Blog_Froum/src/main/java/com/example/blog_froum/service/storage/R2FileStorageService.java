package com.example.blog_froum.service.storage;

import com.example.blog_froum.config.storage.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;

@Slf4j
public class R2FileStorageService implements FileStorageService {

    private final StorageProperties properties;
    private final S3Client s3Client;

    public R2FileStorageService(StorageProperties properties) {
        this.properties = properties;
        validateConfig(properties.getR2());
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(getEndpoint(properties.getR2().getAccountId())))
                .region(Region.of(firstText(properties.getR2().getRegion(), "auto")))
                .serviceConfiguration(S3Configuration.builder()
                        .chunkedEncodingEnabled(false)
                        .build())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                properties.getR2().getAccessKeyId(),
                                properties.getR2().getSecretAccessKey())))
                .build();
    }

    @Override
    public StoredFile store(MultipartFile file, String relativePath, String fileName, String originalName) throws IOException {
        String key = buildObjectKey(relativePath);
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(properties.getR2().getBucket())
                .key(key)
                .contentLength(file.getSize());

        if (StringUtils.hasText(file.getContentType())) {
            requestBuilder.contentType(file.getContentType());
        }

        s3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        String publicUrl = trimTrailingSlash(properties.getR2().getPublicUrl()) + "/" + key;
        log.info("R2 文件上传成功，bucket: {}, key: {}, URL: {}", properties.getR2().getBucket(), key, publicUrl);
        return new StoredFile(fileName, publicUrl, normalizePath(relativePath), originalName);
    }

    @Override
    public void delete(String relativePath) {
        String key = buildObjectKey(relativePath);
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getR2().getBucket())
                .key(key)
                .build());
        log.info("R2 文件删除成功，bucket: {}, key: {}", properties.getR2().getBucket(), key);
    }

    private String buildObjectKey(String relativePath) {
        String prefix = normalizePath(properties.getR2().getPrefix());
        String path = normalizePath(relativePath);
        if (!StringUtils.hasText(prefix)) {
            return path;
        }
        return prefix + "/" + path;
    }

    private String normalizePath(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String getEndpoint(String accountId) {
        return "https://" + accountId + ".r2.cloudflarestorage.com";
    }

    private void validateConfig(StorageProperties.R2 r2) {
        if (!StringUtils.hasText(r2.getAccountId())
                || !StringUtils.hasText(r2.getAccessKeyId())
                || !StringUtils.hasText(r2.getSecretAccessKey())
                || !StringUtils.hasText(r2.getBucket())
                || !StringUtils.hasText(r2.getPublicUrl())) {
            throw new IllegalStateException("R2 存储配置不完整，请设置 FILE_STORAGE_TYPE=r2 以及 R2_ACCOUNT_ID/R2_ACCESS_KEY_ID/R2_SECRET_ACCESS_KEY/R2_BUCKET/R2_PUBLIC_URL");
        }
    }
}
