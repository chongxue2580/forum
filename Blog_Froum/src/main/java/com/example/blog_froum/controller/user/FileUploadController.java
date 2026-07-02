package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.upload.FileUploadResponse;
import com.example.blog_froum.service.storage.FileStorageService;
import com.example.blog_froum.service.storage.StoredFile;
import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "文件上传")
public class FileUploadController {

    private static final long IMAGE_MAX_SIZE = 5 * 1024 * 1024;
    private static final long AVATAR_MAX_SIZE = 2 * 1024 * 1024;
    private static final long DOCUMENT_MAX_SIZE = 10 * 1024 * 1024;

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 上传图片
     */
    @PostMapping("/image")
    @ApiOperation(value = "上传图片", notes = "上传图片文件，支持jpg、png、gif格式")
    public Result<FileUploadResponse> uploadImage(
            @ApiParam(value = "图片文件", required = true) @RequestParam("file") MultipartFile file) {
        try {
            ensureLoggedIn();
            validateFile(file, IMAGE_MAX_SIZE, "文件大小不能超过5MB");
            validateImage(file);

            String fileName = UUID.randomUUID().toString() + getExtension(file.getOriginalFilename());
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String relativePath = "images/" + dateDir + "/" + fileName;

            StoredFile storedFile = fileStorageService.store(file, relativePath, fileName, null);
            log.info("用户 {} 上传图片成功，文件名: {}, URL: {}", BaseContext.getCurrentId(), fileName, storedFile.getUrl());
            return Result.success(toResponse(storedFile));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("上传图片失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    @ApiOperation(value = "上传头像", notes = "上传用户头像，支持jpg、png格式")
    public Result<FileUploadResponse> uploadAvatar(
            @ApiParam(value = "头像文件", required = true) @RequestParam("file") MultipartFile file) {
        try {
            ensureLoggedIn();
            validateFile(file, AVATAR_MAX_SIZE, "头像文件大小不能超过2MB");
            validateAvatar(file);

            String fileName = "avatar_" + BaseContext.getCurrentId() + "_" + System.currentTimeMillis()
                    + getExtension(file.getOriginalFilename());
            String relativePath = "images/avatars/" + fileName;

            StoredFile storedFile = fileStorageService.store(file, relativePath, fileName, null);
            log.info("用户 {} 上传头像成功，文件名: {}, URL: {}", BaseContext.getCurrentId(), fileName, storedFile.getUrl());
            return Result.success(toResponse(storedFile));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("上传头像失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文档
     */
    @PostMapping("/document")
    @ApiOperation(value = "上传文档", notes = "上传文档文件，支持pdf、doc、docx、txt格式")
    public Result<FileUploadResponse> uploadDocument(
            @ApiParam(value = "文档文件", required = true) @RequestParam("file") MultipartFile file) {
        try {
            ensureLoggedIn();
            validateFile(file, DOCUMENT_MAX_SIZE, "文档文件大小不能超过10MB");
            validateDocument(file);

            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + getExtension(originalFilename);
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String relativePath = "documents/" + dateDir + "/" + fileName;

            StoredFile storedFile = fileStorageService.store(file, relativePath, fileName, originalFilename);
            log.info("用户 {} 上传文档成功，文件名: {}, URL: {}", BaseContext.getCurrentId(), fileName, storedFile.getUrl());
            return Result.success(toResponse(storedFile));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("上传文档失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{relativePath:.+}")
    @ApiOperation(value = "删除文件", notes = "删除已上传的文件")
    public Result<Void> deleteFile(
            @ApiParam(value = "文件相对路径", required = true) @PathVariable String relativePath) {
        try {
            ensureLoggedIn();
            fileStorageService.delete(relativePath);
            log.info("用户 {} 删除文件成功，路径: {}", BaseContext.getCurrentId(), relativePath);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            log.error("删除文件失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * Worker 私有图片访问鉴权。
     */
    @GetMapping("/r2-auth")
    @ApiOperation(value = "R2私有资源鉴权", notes = "供 Cloudflare Worker 校验私有对象访问权限")
    public ResponseEntity<Void> authorizeR2Object(@RequestParam("key") String key) {
        ensureLoggedIn();
        if (!StringUtils.hasText(key)) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = BaseContext.getCurrentId();
        String normalizedKey = key.replace("\\", "/").replaceAll("^/+", "");
        if (normalizedKey.startsWith("private/users/" + userId + "/")) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.status(403).build();
    }

    private void ensureLoggedIn() {
        if (BaseContext.getCurrentId() == null) {
            throw new IllegalArgumentException("用户未登录");
        }
    }

    private void validateFile(MultipartFile file, long maxSize, String maxSizeMessage) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(maxSizeMessage);
        }
    }

    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("只支持图片文件");
        }
    }

    private void validateAvatar(MultipartFile file) {
        String contentType = file.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new IllegalArgumentException("头像只支持JPG和PNG格式");
        }
    }

    private void validateDocument(MultipartFile file) {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        boolean validContentType = "application/pdf".equals(contentType)
                || "application/msword".equals(contentType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)
                || "text/plain".equals(contentType);

        if (!validContentType && StringUtils.hasText(originalFilename)) {
            String lowerName = originalFilename.toLowerCase(Locale.ROOT);
            validContentType = lowerName.endsWith(".pdf") || lowerName.endsWith(".doc")
                    || lowerName.endsWith(".docx") || lowerName.endsWith(".txt");
        }

        if (!validContentType) {
            throw new IllegalArgumentException("只支持PDF、DOC、DOCX、TXT格式的文档");
        }
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    private FileUploadResponse toResponse(StoredFile storedFile) {
        return new FileUploadResponse(
                storedFile.getFileName(),
                storedFile.getUrl(),
                storedFile.getRelativePath(),
                storedFile.getOriginalName());
    }
}
