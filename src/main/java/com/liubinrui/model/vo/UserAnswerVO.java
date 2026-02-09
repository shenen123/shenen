package com.liubinrui.model.vo;
import com.liubinrui.model.entity.UserAnswer;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户答题记录视图
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@Data
public class UserAnswerVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 用户答案（JSON 数组）
     */
    private List<String> choices;

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
     * @param user_answerVO
     * @return
     */
    public static UserAnswer voToObj(UserAnswerVO user_answerVO) {
        if (user_answerVO == null) {
            return null;
        }
        UserAnswer user_answer = new UserAnswer();
        BeanUtils.copyProperties(user_answerVO, user_answer);
        return user_answer;
    }

    /**
     * 对象转封装类
     *
     * @param user_answer
     * @return
     */
    public static UserAnswerVO objToVo(UserAnswer user_answer) {
        if (user_answer == null) {
            return null;
        }
        UserAnswerVO user_answerVO = new UserAnswerVO();
        BeanUtils.copyProperties(user_answer, user_answerVO);
        return user_answerVO;
    }
}
