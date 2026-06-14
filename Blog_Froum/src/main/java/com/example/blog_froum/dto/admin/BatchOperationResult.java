package com.example.blog_froum.dto.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果
 */
@Data
public class BatchOperationResult {
    private int total;
    private int succeeded;
    private int failed;
    private List<String> errors = new ArrayList<>();

    public void addSuccess() {
        succeeded++;
    }

    public void addFailure(String error) {
        failed++;
        errors.add(error);
    }
}
