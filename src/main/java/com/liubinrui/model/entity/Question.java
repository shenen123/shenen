package com.liubinrui.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Data;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@TableName(value = "question", autoResultMap = true)
@Data
public class Question implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 应用 id
     */
    private Long appId;
    /**
     * 问题内容
     */
    private String questionContent;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
    // 真正和数据库 column="options" 映射的字段（字符串）
    @TableField("options")
    @JsonIgnore
    private String optionsJson;

    // 不对应数据库字段，由 optionsJson 转换而来
    @TableField(exist = false) // 告诉 MyBatis-Plus：这个字段不在数据库表中
    private List<QuestionOption> options;

    // 自动将 optionsJson ↔ options 双向转换
    public List<QuestionOption> getOptions() {
        if (this.options != null) {
            return this.options;
        }
        if (this.optionsJson == null || this.optionsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.options = objectMapper.readValue(
                    this.optionsJson,
                    new TypeReference<List<QuestionOption>>() {
                    }
            );
            return this.options;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse options JSON", e);
        }
    }

    public void setOptions(List<QuestionOption> options) {
        this.options = options;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.optionsJson = objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize options to JSON", e);
        }
    }

    private static final long serialVersionUID = 1L;
}
