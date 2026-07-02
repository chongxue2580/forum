package com.example.blog_froum.config.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "file.storage")
public class StorageProperties {

    /**
     * local or r2.
     */
    private String type = "local";

    private Local local = new Local();

    private R2 r2 = new R2();

    @Data
    public static class Local {
        private String path = "./uploads";
        private String urlPrefix = "http://localhost:8080/uploads";
    }

    @Data
    public static class R2 {
        private String accountId;
        private String accessKeyId;
        private String secretAccessKey;
        private String bucket;
        private String publicUrl;
        private String prefix = "";
        private String region = "auto";
    }
}
