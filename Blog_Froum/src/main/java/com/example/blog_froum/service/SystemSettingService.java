package com.example.blog_froum.service;

import java.util.Map;

public interface SystemSettingService {

    Map<String, Object> getSettings();

    Map<String, Object> updateSettings(Map<String, Object> settings);

    Map<String, Object> resetToDefaults();

    void sendTestEmail(String recipient);

    int clearCache();
}
