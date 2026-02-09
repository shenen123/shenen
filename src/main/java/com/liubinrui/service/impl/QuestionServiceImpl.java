package com.liubinrui.service.impl;

import java.util.Date;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.liubinrui.common.ErrorCode;
import com.liubinrui.constant.CommonConstant;
import com.liubinrui.exception.ThrowUtils;
import com.liubinrui.mapper.QuestionMapper;
import com.liubinrui.model.entity.Question;
import com.liubinrui.model.entity.QuestionOption;
import com.liubinrui.model.entity.User;
import com.liubinrui.model.vo.QuestionVO;
import com.liubinrui.model.vo.UserVO;
import com.liubinrui.service.QuestionService;
import com.liubinrui.service.UserService;
import com.liubinrui.util.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import com.liubinrui.model.dto.question.QuestionQueryRequest;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 题目服务实现

 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Resource
    private UserService userService;

    /**
     * 校验数据
     *
     * @param question
     * @param add      对创建的数据进行校验
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        ThrowUtils.throwIf(question == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String questionContent = question.getQuestionContent();
        List<QuestionOption> options = question.getOptions();
        Long userId = question.getUserId();
        // 创建数据时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(questionContent), ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(CollUtil.isEmpty(options), ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(ObjUtil.isNull(userId), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        if (StringUtils.isNotBlank(questionContent)) {
            ThrowUtils.throwIf(questionContent.length() > 200, ErrorCode.PARAMS_ERROR, "标题过长");
        }
    }

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = questionQueryRequest.getId();
        Long appId = questionQueryRequest.getAppId();
        String questionContent = questionQueryRequest.getQuestionContent();
        List<QuestionOption> options = questionQueryRequest.getOptions();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        Long userId = questionQueryRequest.getUserId();

        // todo 补充需要的查询条件
        // 从多字段中搜索
        // 模糊查询   WHERE questionContent LIKE '%xxx%'
        queryWrapper.like(StringUtils.isNotBlank(questionContent), "questionContent",questionContent );
        // JSON 数组查询  AND JSON_CONTAINS(`options`, '{"key":"A","value":"正确"}', '$')
        if (CollUtil.isNotEmpty(options)) {
            for (QuestionOption option : options) {
                if (StringUtils.isNoneBlank(option.getKey(), option.getValue())) {
                    String jsonFragment = String.format("{\"key\":\"%s\",\"value\":\"%s\"}",
                            option.getKey(), option.getValue());
                    // 使用 JSON_CONTAINS 安全查询
                    queryWrapper.apply("JSON_CONTAINS(`options`, {0}, '$')", jsonFragment);
                }
            }
        }
        // 精确查询
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(appId != null && appId > 0, "app_id", appId);
        queryWrapper.eq(userId != null && userId > 0, "user_id", userId);
        // 排序规则 sortField = "createTime" 且 sortOrder = "asc"  ORDER BY createTime ASC
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        // 对象转封装类
        QuestionVO questionVO = QuestionVO.objToVo(question);
        // 根据需要为封装对象补充值，不需要的内容可以删除
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUser(userVO);
        return questionVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 对象列表 => 封装对象列表
        List<QuestionVO> questionVOList = questionList.stream().map(question -> QuestionVO.objToVo(question)).collect(Collectors.toList());

        // 根据需要为封装对象补充值，不需要的内容可以删除
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        questionVOList.forEach(questionVO -> {
            Long userId = questionVO.getUserId();
            User user = null;
            if (userMap.containsKey(userId)) {
                user = userMap.get(userId);
            }
            questionVO.setUser(userService.getUserVO(user));
        });
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

}
