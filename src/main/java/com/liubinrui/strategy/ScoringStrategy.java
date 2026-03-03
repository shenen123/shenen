package com.liubinrui.strategy;
import com.liubinrui.model.dto.scoring_result.ScoringResultAddRequest;
import com.liubinrui.model.entity.ScoringResult;

public interface ScoringStrategy {
    /**
     * 判断当前策略是否适用于该 appType
     */
    boolean supports(Integer appType);

    /**
     * 执行评分
     */
    void execute(ScoringResultAddRequest request, ScoringResult result);
}
