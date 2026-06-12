package com.example.blog_froum.repository;

import com.example.blog_froum.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Page<Tag> findAllByOrderByUsageCountDesc(Pageable pageable);

    // 按状态查询标签
    List<Tag> findByStatus(String status);
    
    // 按名称查询标签
    Tag findByName(String name);
    
    // 检查标签名称是否已存在
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
    
    // 查询热门标签（按使用次数排序）
    Page<Tag> findByStatusOrderByUsageCountDesc(String status, Pageable pageable);
    
    // 查询文章相关的标签
    @Query("SELECT t FROM Tag t JOIN t.articles a WHERE a.id = ?1")
    List<Tag> findTagsByArticleId(Long articleId);
} 
