package com.liubinrui.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.liubinrui.common.ErrorCode;
import com.liubinrui.constant.CommonConstant;
import com.liubinrui.exception.ThrowUtils;
import com.liubinrui.mapper.UserAnswerMapper;
import com.liubinrui.model.dto.user_answer.UserAnswerQueryRequest;
import com.liubinrui.model.entity.User;
import com.liubinrui.model.entity.UserAnswer;
import com.liubinrui.model.vo.UserAnswerVO;
import com.liubinrui.model.vo.UserVO;
import com.liubinrui.service.UserAnswerService;
import com.liubinrui.service.UserService;
import com.liubinrui.util.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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
public class UserAnswerServiceImpl extends ServiceImpl<UserAnswerMapper, UserAnswer> implements UserAnswerService {

    @Resource
    private UserService userService;
    @Override
    public void validUserAnswer(UserAnswer userAnswer, boolean add) {
        ThrowUtils.throwIf(userAnswer == null, ErrorCode.PARAMS_ERROR);
        Long appId = userAnswer.getAppId();
        List<String> choices = userAnswer.getChoices();
        Long resultId = userAnswer.getResultId();
        String resultName = userAnswer.getResultName();
        String resultDesc = userAnswer.getResultDesc();
        Integer resultScore = userAnswer.getResultScore();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(ObjectUtils.isEmpty(appId), ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(StringUtils.isBlank(resultName), ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(CollUtil.isEmpty(choices), ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(StringUtils.isBlank(resultDesc), ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(ObjectUtils.isEmpty(resultScore), ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        // todo 补充校验规则
    }

    @Override
    public QueryWrapper<UserAnswer> getQueryWrapper(UserAnswerQueryRequest userAnswerQueryRequest) {
        QueryWrapper<UserAnswer> queryWrapper = new QueryWrapper<>();
        if (userAnswerQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = userAnswerQueryRequest.getId();
        //Long notId = userAnswerQueryRequest.getNotId();
        String searchText = userAnswerQueryRequest.getSearchText();
        Long appId = userAnswerQueryRequest.getAppId();
        String resultName = userAnswerQueryRequest.getResultName();
        String resultDesc = userAnswerQueryRequest.getResultDesc();
        Integer resultScore = userAnswerQueryRequest.getResultScore();
        Long userId = userAnswerQueryRequest.getUserId();
        String sortField = userAnswerQueryRequest.getSortField();
        String sortOrder = userAnswerQueryRequest.getSortOrder();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(resultName), "resultName", resultName);
        queryWrapper.like(StringUtils.isNotBlank(resultDesc), "resultDesc", resultDesc);
        // 精确查询
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(userId != null && userId > 0, "userId", userId);
        queryWrapper.eq(appId != null && appId > 0, "appId", appId);
        queryWrapper.eq(resultScore != null, "resultScore", resultScore);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public UserAnswerVO getUserAnswerVO(UserAnswer useranswer, HttpServletRequest request) {
        // 对象转封装类
        UserAnswerVO user_answerVO = UserAnswerVO.objToVo(useranswer);
        // region 可选
        // 1. 关联查询用户信息
        Long userId = useranswer.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        user_answerVO.setUser(userVO);
        return user_answerVO;
    }

    @Override
    public Page<UserAnswerVO> getUserAnswerVOPage(Page<UserAnswer> useranswerPage, HttpServletRequest request) {
        List<UserAnswer> user_answerList = useranswerPage.getRecords();
        Page<UserAnswerVO> user_answerVOPage = new Page<>(useranswerPage.getCurrent(), useranswerPage.getSize(), useranswerPage.getTotal());
        if (CollUtil.isEmpty(user_answerList)) {
            return user_answerVOPage;
        }
        // 对象列表 => 封装对象列表
        List<UserAnswerVO> userAnswerVOList = user_answerList.stream().map(user_answer -> {
            UserAnswerVO vo = UserAnswerVO.objToVo(user_answer);
            return vo;
        }).collect(Collectors.toList());

        Set<Long> userIdSet = user_answerList.stream().map(UserAnswer::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        // 填充信息
        userAnswerVOList.forEach(userAnswerVO -> {
            Long userId = userAnswerVO.getUserId();
            User user = null;
            if (userMap.containsKey(userId)) {
                user = userMap.get(userId);
            }
            userAnswerVO.setUser(userService.getUserVO(user));

        });
        // endregion
        user_answerVOPage.setRecords(userAnswerVOList);
        return user_answerVOPage;
    }
}
