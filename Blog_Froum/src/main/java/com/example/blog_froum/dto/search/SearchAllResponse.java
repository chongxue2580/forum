package com.example.blog_froum.dto.search;

import com.example.blog_froum.dto.article.ArticleResponse;
import com.example.blog_froum.dto.question.QuestionResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchAllResponse {
    private Page<ArticleResponse> articles;
    private Page<QuestionResponse> questions;
    private String keyword;
}
