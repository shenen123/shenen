package com.liubinrui.enums;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum ScoreLevelEnum {

    EXCELLENT(95, "非常棒", "成绩大于等于90分，表现卓越"),
    GOOD(85, "成绩良好", "成绩在75至89分之间，表现优良"),
    FAIR(60, "成绩不错", "成绩在60至74分之间，达到基本要求"),
    NEED_IMPROVEMENT(0, "仍需努力", "成绩低于60分，需要加强学习");

    private final int minScore;       // 该等级的分数
    private final String text;        // 中文描述（如“非常棒”）
    private final String description; // 详细说明

    ScoreLevelEnum(int minScore, String text, String description) {
        this.minScore = minScore;
        this.text = text;
        this.description = description;
    }
    
    public static ScoreLevelEnum getLevelByScore(Integer score) {
        if (score == null || score < 0 || score > 100) {
            return null;
        }
        // 按分数从高到低排序（确保先匹配高分段）
        for (ScoreLevelEnum level : Arrays.asList(EXCELLENT, GOOD, FAIR, NEED_IMPROVEMENT)) {
            if (score >= level.minScore) {
                return level;
            }
        }
        return NEED_IMPROVEMENT;
    }

    public static List<String> getTexts() {
        return Arrays.stream(values()).map(ScoreLevelEnum::getText).collect(Collectors.toList());
    }

    // Getter 方法
    public int getMinScore() {
        return minScore;
    }

    public String getText() {
        return text;
    }

    public String getDescription() {
        return description;
    }
}
