package com.example.blog_froum.repository;

import com.example.blog_froum.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Page<Category> findAllByOrderByDisplayOrderAsc(Pageable pageable);

    Page<Category> findByStatusOrderByDisplayOrderAsc(String status, Pageable pageable);

    // 按状态查询分类，并按显示顺序排序
    List<Category> findByStatusOrderByDisplayOrder(String status);
    
    // 按名称查询分类
    Category findByName(String name);
    
    // 检查分类名称是否已存在
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
} 
