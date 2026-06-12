package com.example.blog_froum.controller.admin;

import com.example.blog_froum.dto.admin.EmailTestRequest;
import com.example.blog_froum.service.OperationLogService;
import com.example.blog_froum.service.SystemSettingService;
import com.example.blog_froum.utils.BaseContext;
import com.example.blog_froum.utils.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/settings")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5177", "http://localhost:5178", "http://localhost:5179", "http://localhost:5180", "http://localhost:5181", "http://localhost:5182", "http://localhost:5183", "http://localhost:5184", "http://localhost:5185", "http://localhost:8081"})
@Api(tags = "管理员系统设置")
public class AdminSystemSettingsController {

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    @ApiOperation(value = "获取系统设置", notes = "获取当前持久化系统设置")
    public Result<Map<String, Object>> getSettings() {
        try {
            return Result.success(systemSettingService.getSettings());
        } catch (Exception e) {
            log.error("获取系统设置失败", e);
            return Result.error("获取系统设置失败: " + e.getMessage());
        }
    }

    @PutMapping
    @ApiOperation(value = "保存系统设置", notes = "保存当前系统设置")
    public Result<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings,
                                                      HttpServletRequest request) {
        try {
            Map<String, Object> savedSettings = systemSettingService.updateSettings(settings);
            operationLogService.record(BaseContext.getCurrentId(), "SYSTEM_SETTINGS", "update",
                    "更新系统设置", "SYSTEM_SETTINGS", null, "系统设置", request);
            return Result.success("系统设置已保存", savedSettings);
        } catch (Exception e) {
            log.error("保存系统设置失败", e);
            return Result.error("保存系统设置失败: " + e.getMessage());
        }
    }

    @PostMapping("/reset")
    @ApiOperation(value = "重置系统设置", notes = "将系统设置重置为默认值")
    public Result<Map<String, Object>> resetSettings(HttpServletRequest request) {
        try {
            Map<String, Object> settings = systemSettingService.resetToDefaults();
            operationLogService.record(BaseContext.getCurrentId(), "SYSTEM_SETTINGS", "reset",
                    "重置系统设置为默认值", "SYSTEM_SETTINGS", null, "系统设置", request);
            return Result.success("系统设置已重置", settings);
        } catch (Exception e) {
            log.error("重置系统设置失败", e);
            return Result.error("重置系统设置失败: " + e.getMessage());
        }
    }

    @PostMapping("/email/test")
    @ApiOperation(value = "测试邮件设置", notes = "使用当前SMTP设置发送测试邮件")
    public Result<Void> testEmail(@RequestBody(required = false) EmailTestRequest emailTestRequest,
                                  HttpServletRequest request) {
        try {
            String recipient = emailTestRequest == null ? null : emailTestRequest.getRecipient();
            systemSettingService.sendTestEmail(recipient);
            operationLogService.record(BaseContext.getCurrentId(), "SYSTEM_SETTINGS", "email_test",
                    "发送系统邮件测试", "SYSTEM_SETTINGS", null, "邮件设置", request);
            return Result.success("测试邮件已发送");
        } catch (Exception e) {
            log.error("测试邮件设置失败", e);
            return Result.error("测试邮件发送失败: " + e.getMessage());
        }
    }

    @PostMapping("/cache/clear")
    @ApiOperation(value = "清除系统缓存", notes = "清除已启用缓存管理器中的缓存")
    public Result<Integer> clearCache(HttpServletRequest request) {
        try {
            int clearedCaches = systemSettingService.clearCache();
            operationLogService.record(BaseContext.getCurrentId(), "SYSTEM_SETTINGS", "clear_cache",
                    "清除系统缓存，缓存数量：" + clearedCaches, "SYSTEM_SETTINGS", null, "缓存设置", request);
            return Result.success("系统缓存已清除", clearedCaches);
        } catch (Exception e) {
            log.error("清除系统缓存失败", e);
            return Result.error("清除系统缓存失败: " + e.getMessage());
        }
    }
}
