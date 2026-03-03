package com.liubinrui.manager;

import com.liubinrui.strategy.QuestionGenerationStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuestionGenerationStrategyManager {

    private final Map<Integer, QuestionGenerationStrategy> strategyMap;

    public QuestionGenerationStrategyManager(List<QuestionGenerationStrategy> strategies) {
        this.strategyMap = strategies.stream()
                //Collectors.toMap(keyMapper, valueMapper, mergeFunction)
                //keyMapper: 如何从每个元素提取 key
                //→ QuestionGenerationStrategy::getAppType
                //valueMapper: 如何从每个元素提取 value → Function.identity(),表示：value 就是策略对象本身（即 strategy -> strategy
                .collect(Collectors.toMap(
                        QuestionGenerationStrategy::getAppType,
                        Function.identity(),
                        (existing, replacement) -> existing // 防止重复
                ));
    }

    public QuestionGenerationStrategy getStrategyByAppType(Integer appType) {
        return strategyMap.get(appType);
    }
}
