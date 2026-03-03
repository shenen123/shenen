package com.liubinrui.model.dto.scoring_result;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScoringResultAddRequest implements Serializable {
    /**
     * 结果名称，如物流师
     */
    private String resultName;

    /**
     * 结果描述
     */
    private String resultDesc;

    /**
     * 结果属性集合 JSON，如 [I,S,T,J]
     */
    private String resultProp;

    /**
     * 结果得分
     */
    private Integer resultScore;

    /**
     * 应用 id
     */
    private Long appId;
    /**
     * 回答id
     */
    private Long answerId;
    private static final long serialVersionUID = 1L;
}