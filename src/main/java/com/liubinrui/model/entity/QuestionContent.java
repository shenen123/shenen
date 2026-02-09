package com.liubinrui.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionContent {
    //用于AI生成题目
    private String title;

    private List<QuestionOption> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Option {
        private String result;
        private int score;
        private String value;
        private String key;
    }
}

