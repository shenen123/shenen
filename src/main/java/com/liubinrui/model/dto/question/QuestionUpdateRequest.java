package com.liubinrui.model.dto.question;

import com.liubinrui.model.entity.QuestionOption;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新题目请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Data
public class QuestionUpdateRequest implements Serializable {
    /**
     * id
     */
    private Long id;
    /**
     * 应用 id
     */
    private Long appId;

    private String questionContent;

    private List<QuestionOption> options;


    private static final long serialVersionUID = 1L;
}