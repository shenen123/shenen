package com.liubinrui.model.dto.scoring_result;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScoringResultCreateRequest implements Serializable {

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
