package com.liubinrui.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liubinrui.common.ErrorCode;
import com.liubinrui.constant.CommonConstant;
import com.liubinrui.exception.ThrowUtils;
import com.liubinrui.mapper.QuestionMapper;
import com.liubinrui.mapper.ScoringResultMapper;
import com.liubinrui.mapper.UserAnswerMapper;
import com.liubinrui.model.dto.scoring_result.ScoringResultAddRequest;
import com.liubinrui.model.dto.scoring_result.ScoringResultQueryRequest;
import com.liubinrui.model.entity.*;
import com.liubinrui.model.vo.ScoringResultVO;
import com.liubinrui.model.vo.UserVO;
import com.liubinrui.service.AppService;
import com.liubinrui.service.ScoringResultService;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScoringResultServiceImpl extends ServiceImpl<ScoringResultMapper, ScoringResult> implements ScoringResultService {

    @Resource
    private UserService userService;
    @Resource
    private AppService appService;
    @Resource
    private UserAnswerService userAnswerService;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private UserAnswerMapper userAnswerMapper;

    /**
     * 校验数据
     * @param scoring_result
     * @param add
     */
    @Override
    public void validScoringResult(ScoringResult scoring_result, boolean add) {
        ThrowUtils.throwIf(scoring_result == null, ErrorCode.PARAMS_ERROR);
        // todo 从对象中取值
        String resultName = scoring_result.getResultName();
        String resultDesc = scoring_result.getResultDesc();
        String resultProp = scoring_result.getResultProp();
        log.info("liubinrui:" + scoring_result);
        Long appId = scoring_result.getAppId();
        // 创建数据时，参数不能为空
        if (add) {
            // todo 补充校验规则
            ThrowUtils.throwIf(StringUtils.isBlank(resultName), ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(StringUtils.isBlank(resultDesc), ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(ObjectUtils.isEmpty(appId), ErrorCode.PARAMS_ERROR);
        }
    }

    @Override
    public QueryWrapper<ScoringResult> getQueryWrapper(ScoringResultQueryRequest scoring_resultQueryRequest) {
        QueryWrapper<ScoringResult> queryWrapper = new QueryWrapper<>();
        if (scoring_resultQueryRequest == null) {
            return queryWrapper;
        }
        // todo 从对象中取值
        Long id = scoring_resultQueryRequest.getId();
        Long notId = scoring_resultQueryRequest.getNotId();
        String searchText = scoring_resultQueryRequest.getSearchText();
        String resultName = scoring_resultQueryRequest.getResultName();
        String resultDesc = scoring_resultQueryRequest.getResultDesc();
        String resultProp = scoring_resultQueryRequest.getResultProp();
        Integer resultScoreRange = scoring_resultQueryRequest.getResultScoreRange();
        Long appId = scoring_resultQueryRequest.getAppId();
        Long userId = scoring_resultQueryRequest.getUserId();
        String sortField = scoring_resultQueryRequest.getSortField();
        String sortOrder = scoring_resultQueryRequest.getSortOrder();
        // todo 补充需要的查询条件
        // 从多字段中搜索
        if (StringUtils.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("result_name", searchText).or().like("result_desc", searchText));
        }
        // 模糊查询
        queryWrapper.like(StringUtils.isNotBlank(resultName), "result_name", resultName);
        queryWrapper.like(StringUtils.isNotBlank(resultDesc), "result_desc", resultDesc);
        queryWrapper.like(StringUtils.isNotBlank(resultProp), "result_prop", resultProp);
        // 精确查询
        queryWrapper.eq(appId != null && appId > 0, "app_id", appId);
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(resultScoreRange != null && resultScoreRange > 0, "resultScoreRange", resultScoreRange);
        queryWrapper.eq(userId != null && userId > 0, "userId", userId);
        // 排序规则
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public ScoringResultVO getScoringResultVO(ScoringResult scoring_result, HttpServletRequest request) {
        // 对象转封装类
        ScoringResultVO scoring_resultVO = ScoringResultVO.objToVo(scoring_result);
        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // 1. 关联查询用户信息
        Long userId = scoring_result.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        scoring_resultVO.setUser(userVO);
        return scoring_resultVO;
    }

    @Override
    public Page<ScoringResultVO> getScoringResultVOPage(Page<ScoringResult> scoring_resultPage, HttpServletRequest request) {
        List<ScoringResult> scoring_resultList = scoring_resultPage.getRecords();
        Page<ScoringResultVO> scoring_resultVOPage = new Page<>(scoring_resultPage.getCurrent(), scoring_resultPage.getSize(), scoring_resultPage.getTotal());
        if (CollUtil.isEmpty(scoring_resultList)) {
            return scoring_resultVOPage;
        }
        // 对象列表 => 封装对象列表
        List<ScoringResultVO> scoring_resultVOList = scoring_resultList.stream().map(scoring_result -> {
            return ScoringResultVO.objToVo(scoring_result);
        }).collect(Collectors.toList());

        // todo 可以根据需要为封装对象补充值，不需要的内容可以删除
        // 1. 关联查询用户信息
        Set<Long> userIdSet = scoring_resultList.stream().map(ScoringResult::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        scoring_resultVOList.forEach(scoring_resultVO -> {
            Long userId = scoring_resultVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            scoring_resultVO.setUser(userService.getUserVO(user));
        });
        scoring_resultVOPage.setRecords(scoring_resultVOList);
        return scoring_resultVOPage;
    }
    @Override
    public Integer createIntegerScoringResult(ScoringResultAddRequest scoringResultAddRequest) {
        // 参数校验
        ThrowUtils.throwIf(scoringResultAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = scoringResultAddRequest.getAppId();
        Long answerId = scoringResultAddRequest.getAnswerId();
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(answerId == null || answerId < 0, ErrorCode.PARAMS_ERROR);

        // 获取应用、题目、用户答案
        App app = appService.getById(appId);
        List<Question> questions = questionMapper.getByAppId(appId);
        UserAnswer userAnswer = userAnswerService.getById(answerId);
        List<String> choices = userAnswer.getChoices();

        // 校验数据完整性
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(questions == null || questions.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "题目未找到");
        ThrowUtils.throwIf(userAnswer == null, ErrorCode.NOT_FOUND_ERROR, "用户答案不存在");
        ThrowUtils.throwIf(choices == null || choices.isEmpty(), ErrorCode.PARAMS_ERROR, "用户未作答");

        log.info("回答题目数量：{}",choices.size());
        log.info("题目总数量：{}",questions.size());
        // 题目数和选项数应匹配
        ThrowUtils.throwIf(choices.size() != questions.size(), ErrorCode.PARAMS_ERROR, "题目与答案数量不一致");

        int totalScore = 0;

        // 遍历每一道题和对应的用户选择
        for (int i = 0; i < choices.size(); i++) {
            String selectedKey = choices.get(i); // 用户选择的选项 key，如 "A"
            Question question = questions.get(i);
            List<QuestionOption> options = question.getOptions();

            // 查找用户选择的选项，并累加分数
            boolean found = false;
            for (QuestionOption option : options) {
                if (option.getKey().equals(selectedKey)) {
                    totalScore += option.getScore(); // 假设 QuestionOption 有 getScore() 返回 Integer 或 int
                    found = true;
                    break;
                }
            }
            // 如果没找到匹配的选项，可视为异常（或跳过，根据业务决定）
            ThrowUtils.throwIf(!found, ErrorCode.PARAMS_ERROR, "第 " + (i + 1) + " 题的选择无效: " + selectedKey);
        }

        return totalScore;
    }
    @Override
    public String createStringScoringResult(ScoringResultAddRequest scoringResultAddRequest) {
        //todo 如果数据库已经创建了，就不再创建
        ThrowUtils.throwIf(scoringResultAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = scoringResultAddRequest.getAppId();
        Long answerId = scoringResultAddRequest.getAnswerId();
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(answerId == null || answerId < 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(appId);
        List<Question> questions = questionMapper.getByAppId(appId);
        UserAnswer userAnswer = userAnswerService.getById(answerId);
        List<String> choices = userAnswer.getChoices();
        //遍历选项和,计算MBIT的类型
        String s = null;
        s = calculateMBTIType(choices, questions);
        return s;
    }

    private String calculateMBTIType(List<String> choices, List<Question> apps) {
        // 1. 定义维度顺序（必须和计分数组索引一致）
        String[] dimensions = {"I", "E", "S", "N", "T", "F", "J", "P"};
        int[] scores = new int[8]; // 初始化为0
        log.info("choices:" + choices);
        log.info("apps:" + apps);
        log.info("choices数量为:" + choices.size());
        log.info("apps数量为:" + apps.size());
        // 2. 遍历每一道题和对应的用户选择
        for (int i = 0; i < choices.size(); i++) {
            String userChoice = choices.get(i);
            Question question = apps.get(i);
            List<QuestionOption> questionOptions = question.getOptions();
            log.info("option:" + questionOptions);
            // 在该题的 options 中查找 key 匹配的选项
            for (QuestionOption option : question.getOptions()) {
                if (userChoice.equals(option.getKey())) {
                    String result = option.getResult();
                    // 找到 result 在 dimensions 中的位置并加分
                    for (int j = 0; j < dimensions.length; j++) {
                        if (dimensions[j].equals(result)) {
                            scores[j]++;
                            break; // 找到就跳出内层循环
                        }
                    }
                    break; // 找到匹配 option 后跳出 options 循环，进入下一题
                }
            }
        }
        // 3. 构建最终 MBTI 类型（四对维度比较）
        StringBuilder mbti = new StringBuilder();
        // I vs E
        log.info("{}-{}", scores[0], scores[1]);
        mbti.append(scores[0] >= scores[1] ? "I" : "E");
        // S vs N
        log.info("{}-{}", scores[2], scores[3]);
        mbti.append(scores[2] >= scores[3] ? "S" : "N");
        // T vs F
        log.info("{}-{}", scores[4], scores[5]);
        mbti.append(scores[4] >= scores[5] ? "T" : "F");
        // J vs P
        log.info("{}-{}", scores[6], scores[7]);
        mbti.append(scores[6] >= scores[7] ? "J" : "P");
        return mbti.toString();
    }
}
