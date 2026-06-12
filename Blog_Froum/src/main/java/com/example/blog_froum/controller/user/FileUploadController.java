package com.example.blog_froum.controller.user;

import com.example.blog_froum.dto.upload.FileUploadResponse;
import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:http://localhost:8080/uploads}")
    private String urlPrefix;

    /**
     * 上传图片
     */
    @PostMapping("/image")
    @ApiOperation(value = "上传图片", notes = "上传图片文件，支持jpg、png、gif格式")
    public Result<FileUploadResponse> uploadImage(
            @ApiParam(value = "图片文件", required = true) @RequestParam("file") MultipartFile file) {
        try {
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            // 验证文件
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只支持图片文件");
            }

            // 验证文件大小（5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("文件大小不能超过5MB");
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String fileName = UUID.randomUUID().toString() + extension;
            
            // 创建目录结构：uploads/images/yyyy/MM/dd/
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String relativePath = "images/" + dateDir + "/" + fileName;

            // 创建完整路径 - 使用绝对路径
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().resolve("images").resolve(dateDir);
            Files.createDirectories(uploadDir);

            // 保存文件
            Path filePath = uploadDir.resolve(fileName);
            log.info("保存文件到: {}", filePath.toAbsolutePath());
            file.transferTo(filePath.toFile());
            
            // 生成访问URL
            String fileUrl = urlPrefix + "/" + relativePath.replace("\\", "/");
            
            log.info("用户 {} 上传图片成功，文件名: {}, URL: {}", userId, fileName, fileUrl);
            
            FileUploadResponse result = new FileUploadResponse(fileName, fileUrl, relativePath, null);
            
            return Result.success(result);
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
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            // 验证文件
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || 
                (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
                return Result.error("头像只支持JPG和PNG格式");
            }

            // 验证文件大小（2MB）
            if (file.getSize() > 2 * 1024 * 1024) {
                return Result.error("头像文件大小不能超过2MB");
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + extension;
            
            // 创建目录结构：uploads/avatars/
            String relativePath = "avatars/" + fileName;
            
            // 创建完整路径
            Path uploadDir = Paths.get(uploadPath, "avatars");
            Files.createDirectories(uploadDir);
            
            // 保存文件
            Path filePath = uploadDir.resolve(fileName);
            file.transferTo(filePath.toFile());
            
            // 生成访问URL
            String fileUrl = urlPrefix + "/" + relativePath.replace("\\", "/");
            
            log.info("用户 {} 上传头像成功，文件名: {}, URL: {}", userId, fileName, fileUrl);
            
            FileUploadResponse result = new FileUploadResponse(fileName, fileUrl, relativePath, null);
            
            return Result.success(result);
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
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            // 验证文件
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            // 验证文件类型
            String contentType = file.getContentType();
            String originalFilename = file.getOriginalFilename();
            
            boolean isValidType = false;
            if (contentType != null) {
                isValidType = contentType.equals("application/pdf") ||
                             contentType.equals("application/msword") ||
                             contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                             contentType.equals("text/plain");
            }
            
            if (!isValidType && originalFilename != null) {
                String extension = originalFilename.toLowerCase();
                isValidType = extension.endsWith(".pdf") || extension.endsWith(".doc") || 
                             extension.endsWith(".docx") || extension.endsWith(".txt");
            }
            
            if (!isValidType) {
                return Result.error("只支持PDF、DOC、DOCX、TXT格式的文档");
            }

            // 验证文件大小（10MB）
            if (file.getSize() > 10 * 1024 * 1024) {
                return Result.error("文档文件大小不能超过10MB");
            }

            // 生成文件名
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String fileName = UUID.randomUUID().toString() + extension;
            
            // 创建目录结构：uploads/documents/yyyy/MM/
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String relativePath = "documents/" + dateDir + "/" + fileName;
            
            // 创建完整路径
            Path uploadDir = Paths.get(uploadPath, "documents", dateDir);
            Files.createDirectories(uploadDir);
            
            // 保存文件
            Path filePath = uploadDir.resolve(fileName);
            file.transferTo(filePath.toFile());
            
            // 生成访问URL
            String fileUrl = urlPrefix + "/" + relativePath.replace("\\", "/");
            
            log.info("用户 {} 上传文档成功，文件名: {}, URL: {}", userId, fileName, fileUrl);
            
            FileUploadResponse result = new FileUploadResponse(fileName, fileUrl, relativePath, originalFilename);
            
            return Result.success(result);
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
            Long userId = BaseContext.getCurrentId();
            if (userId == null) {
                return Result.error("用户未登录");
            }

            // 构建并校验文件路径，防止通过 ../ 删除上传根目录以外的文件。
            Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(relativePath).normalize();
            if (!filePath.startsWith(basePath)) {
                return Result.error("非法文件路径");
            }
            
            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                return Result.error("文件不存在");
            }
            
            // 删除文件
            Files.delete(filePath);
            
            log.info("用户 {} 删除文件成功，路径: {}", userId, relativePath);
            return Result.success();
        } catch (IOException e) {
            log.error("删除文件失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
