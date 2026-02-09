package com.liubinrui.model.entity;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 用户答题记录
 * @TableName user_answer
 */
@TableName(value ="user_answer")
@Data
public class UserAnswer implements Serializable {
    /**
     *
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 应用类型（0-得分类，1-角色测评类）
     */
    private Integer appType;

    /**
     * 评分策略（0-自定义，1-AI）
     */
    private Integer scoringStrategy;

    /**
     * 用户答案（JSON 数组）
     */
    @TableField("choices") // 显式指定列名为 choices
    @JsonIgnore
    private String choicesDb; // 或叫 choicesJson，避免混淆

    // 2. 【逻辑字段】对外暴露 List<String>，但不参与 MP 映射
    @TableField(exist = false) // 关键！告诉 MP：这个字段不存在于数据库
    private List<String> choices;

    // 3. 重写 getChoices()：从 choicesDb 解析
    public List<String> getChoices() {
        if (this.choices != null) {
            return this.choices; // 支持手动 set
        }
        if (StrUtil.isBlank(choicesDb)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(choicesDb, String.class);
        } catch (Exception e) {
            // 日志可选
            return new ArrayList<>();
        }
    }

    // 4. 重写 setChoices()：序列化到 choicesDb
    public void setChoices(List<String> choices) {
        this.choices = choices; // 缓存，避免重复解析
        this.choicesDb = JSONUtil.toJsonStr(choices);
    }

    // 5. 提供对 choicesDb 的访问（MP 内部使用）
    public String getChoicesDb() {
        return choicesDb;
    }

    public void setChoicesDb(String choicesDb) {
        this.choicesDb = choicesDb;
        this.choices = null; // 清缓存，下次 get 时重新解析
    }

    /**
     * 评分结果 id
     */
    private Long resultId;

    /**
     * 结果名称，如物流师
     */
    private String resultName;

    /**
     * 结果描述
     */
    private String resultDesc;

    /**
     * 结果图标
     */
    private String resultPicture;

    /**
     * 得分
     */
    private Integer resultScore;

    /**
     * 用户 id
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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
