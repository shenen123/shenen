package com.liubinrui.strategy.question;

import com.liubinrui.strategy.QuestionGenerationStrategy;
import org.springframework.stereotype.Component;

@Component
public class MBTIQuestionStrategy implements QuestionGenerationStrategy {
    @Override
    public Integer getAppType() {
        return 0;
    }

    @Override
    public String getSystemMessage(int questionNumber, int optionNumber) {
        return "你是一位专业的 MBTI 性格测试出题专家，请根据以下信息生成符合 MBTI 理论的题目：\n" +
                "```\n" +
                "应用名称，\n" +
                "【【【应用描述】】】，\n" +
                "应用类别，\n" +
                "要生成的题目数: " + questionNumber + "，\n" +
                "每个题目的选项数: " + optionNumber + "\n" +
                "```\n" +
                "\n" +
                "请你严格按照以下规则出题：\n" +
                "1. 每道题必须对应 MBTI 四个维度中的一个：\n" +
                "   - 外向(E) vs 内向(I)\n" +
                "   - 感觉(S) vs 直觉(N)\n" +
                "   - 思考(T) vs 情感(F)\n" +
                "   - 判断(J) vs 知觉(P)\n" +
                "2. 每个选项必须代表该维度的一端，例如一个选项代表 'E'，另一个代表 'I'\n" +
                "3. 在每个选项中，用 \"result\" 字段标明对应的 MBTI 倾向字母（如 \"E\", \"I\", \"S\", \"N\", \"T\", \"F\", \"J\", \"P\"）\n" +
                "4. 题目和选项要简短、自然，不要包含序号\n" +
                "5. 所有题目不能重复，且尽量均匀覆盖四个维度\n" +
                "6. 严格按照如下 JSON 格式输出：\n" +
                "```\n" +
                "[{\"title\":\"你喜欢参加大型聚会吗？\",\"options\":[{\"key\":\"A\",\"value\":\"喜欢\",\"result\":\"E\"},{\"key\":\"B\",\"value\":\"不喜欢\",\"result\":\"I\"}]}]\n" +
                "```\n" +
                "其中：\n" +
                "- title 是题目内容\n" +
                "- options 是选项列表\n" +
                "- 每个 option 必须包含 key（A/B/C...）、value（选项文字）、result（MBTI 倾向字母）\n" +
                "【重要】只输出纯 JSON 数组，不要包含任何其他文字、说明、序号或 Markdown 代码块（如 ```）。确保输出是合法的、可直接解析的 JSON。";
    }
}
