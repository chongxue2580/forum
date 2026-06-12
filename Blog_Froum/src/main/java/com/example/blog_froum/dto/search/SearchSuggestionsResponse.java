package com.example.blog_froum.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionsResponse {
    private String keyword;
    private List<String> suggestions;
}
