package com.liubinrui.model.vo;

import com.liubinrui.model.entity.ScoringResult;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 评分结果视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Data
public class ScoringResultVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 结果名称，如物流师
     */
    private String resultName;

    /**
     * 结果描述
     */
    private String resultDesc;

    /**
     * 结果图片
     */
    private String resultPicture;

    /**
     * 结果属性集合 JSON，如 [I,S,T,J]
     */
    private String resultProp;

    /**
     * 结果得分范围，如 80，表示 80及以上的分数命中此结果
     */
    private Integer resultScoreRange;

    /**
     * 应用 id
     */
    private Long appId;

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
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 封装类转对象
     *
     * @param scoring_resultVO
     * @return
     */
    public static ScoringResult voToObj(ScoringResultVO scoring_resultVO) {
        if (scoring_resultVO == null) {
            return null;
        }
        ScoringResult scoring_result = new ScoringResult();
        BeanUtils.copyProperties(scoring_resultVO, scoring_result);
        return scoring_result;
    }

    /**
     * 对象转封装类
     *
     * @param scoring_result
     * @return
     */
    public static ScoringResultVO objToVo(ScoringResult scoring_result) {
        if (scoring_result == null) {
            return null;
        }
        ScoringResultVO scoring_resultVO = new ScoringResultVO();
        BeanUtils.copyProperties(scoring_result, scoring_resultVO);
        return scoring_resultVO;
    }
}
