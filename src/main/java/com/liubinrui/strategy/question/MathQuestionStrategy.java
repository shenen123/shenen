package com.liubinrui.strategy.question;

import com.liubinrui.model.entity.App;
import com.liubinrui.strategy.QuestionGenerationStrategy;
import org.springframework.stereotype.Component;

@Component
public class MathQuestionStrategy implements QuestionGenerationStrategy {

    @Override
    public Integer getAppType() {
        return 1;
    }

    @Override
    public String getSystemMessage(int questionNumber, int optionNumber) {
        return "你是一位严谨的小学数学出题老师，请严格按照以下规则生成加减乘除四则运算题目：\n" +
                "\n" +
                "【任务要求】\n" +
                "- 生成 " + questionNumber + " 道题目\n" +
                "- 每题提供 " + optionNumber + " 个选项（用 A/B/C/D... 标识）\n" +
                "\n" +
                "【出题规则】\n" +
                "1. 题目必须是整数加减乘除计算题（如 '12 + 8 = ?'），适合小学生，可含两位数或三位数，禁止小数/负数\n" +
                "2. 每题的选项中仅一个正确\n" +
                "3. 【【【强制步骤 - 必须严格执行】】】\n" +
                "   a) 先**准确计算**题目结果（例如：72 / 8 = 9，不是 8！）\n" +
                "   b) 将**正确结果**作为其中一个选项的 value\n" +
                "   c) 为该选项设置 score=5，其余三个干扰项 score=0\n" +
                "   d) **正确答案的位置必须在选项中轮换**\n" +
                "4. 四个选项的 value 必须互不相同，且均为整数字符串（如 \"9\"）\n" +
                "5. 每个选项**只能包含且必须包含以下三个字段**：key（A/B/C/D...）、value（字符串）、score（5 或 0）\n" +
                "6. 【【【严禁】】】添加任何额外字段（如 result、description、id 等），即使值为 null 也不允许！\n" +
                "\n" +
                "【输出格式 - 极其重要】\n" +
                "- 仅输出一个 JSON 数组，以 '[' 开头、']' 结尾\n" +
                "- 禁止任何额外内容：无说明、无序号、无 Markdown、无反引号、无空行\n" +
                "- 所有字符串必须用英文双引号 \"\"\n" +
                "- 确保 JSON 语法 100% 合法\n" +
                "- 示例（正确）：[{\"title\":\"72 / 8 = ?\",\"options\":[{\"key\":\"A\",\"value\":\"8\",\"score\":0}," +
                "{\"key\":\"B\",\"value\":\"9\",\"score\":5},{\"key\":\"C\",\"value\":\"10\",\"score\":0}," +
                "{\"key\":\"D\",\"value\":\"7\",\"score\":0}]}]";
    }

    @Override
    public String getUserMessage(App app, int questionNumber, int optionNumber) {
        return app.getAppName() + "\n" +
                app.getAppDesc() + "\n" +
                "生成 " + questionNumber + " 道小学加减乘除题"+
                "每题"+optionNumber+"个选项";
    }
}
