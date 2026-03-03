package com.liubinrui.strategy;

import com.liubinrui.model.entity.App;

public interface QuestionGenerationStrategy {

    Integer getAppType();

    // 获取系统提示词
    String getSystemMessage(int questionNumber, int optionNumber);

    // 获取用户消息（可选重写）
    default String getUserMessage(App app, int questionNumber, int optionNumber) {
        return app.getAppName() + "\n" +
                app.getAppDesc() + "\n" +
                "题目数: " + questionNumber + ", 选项数: " + optionNumber;
    }

}
