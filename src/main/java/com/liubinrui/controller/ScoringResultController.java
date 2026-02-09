package com.liubinrui.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liubinrui.annotation.AuthCheck;
import com.liubinrui.common.BaseResponse;
import com.liubinrui.common.DeleteRequest;
import com.liubinrui.common.ErrorCode;
import com.liubinrui.common.ResultUtils;
import com.liubinrui.constant.UserConstant;
import com.liubinrui.exception.BusinessException;
import com.liubinrui.exception.ThrowUtils;
import com.liubinrui.model.dto.scoring_result.ScoringResultAddRequest;
import com.liubinrui.model.dto.scoring_result.ScoringResultQueryRequest;
import com.liubinrui.model.dto.scoring_result.ScoringResultUpdateRequest;
import com.liubinrui.model.entity.App;
import com.liubinrui.model.entity.ScoringResult;
import com.liubinrui.model.entity.User;
import com.liubinrui.model.entity.UserAnswer;
import com.liubinrui.model.vo.ScoringResultVO;
import com.liubinrui.service.AppService;
import com.liubinrui.service.QuestionService;
import com.liubinrui.service.UserAnswerService;
import com.liubinrui.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * 评分结果接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/scoringResult")
@Slf4j
public class ScoringResultController {


    @Resource
    private UserService userService;

    @Resource
    private AppService appService;
    @Resource
    private UserAnswerService userAnswerService;

    @PostMapping("/add")
    public BaseResponse<Long> addScoringResult(@RequestBody ScoringResultAddRequest scoringResultAddRequest, HttpServletRequest request) {
        //点击确认自动调用这个接口
        ThrowUtils.throwIf(scoringResultAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        ScoringResult scoringResult = new ScoringResult();
        BeanUtils.copyProperties(scoringResultAddRequest, scoringResult);
        Long appId = scoringResultAddRequest.getAppId();
        App app = appService.getById(appId);
        User loginUser = userService.getLoginUser(request);
        scoringResult.setUserId(loginUser.getId());
        // 根据 appType 选择合适的评分策略
        ScoringStrategy selectedStrategy = scoringStrategyList.stream()
                .filter(strategy -> strategy.supports(app.getAppType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的应用类型: " + app.getAppType()));

        // 执行评分策略，填充 scoringResult
        selectedStrategy.execute(scoringResultAddRequest, scoringResult);
        // 数据校验
        scoringResultService.validScoringResult(scoringResult, true);
        // 写入数据库
        boolean result = scoringResultService.save(scoringResult);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newScoringResultId = scoringResult.getId();
        return ResultUtils.success(newScoringResultId);
    }

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteScoringResult(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        ScoringResult oldScoringResult = scoringResultService.getById(id);
        ThrowUtils.throwIf(oldScoringResult == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = scoringResultService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateScoringResult(@RequestBody ScoringResultUpdateRequest scoring_resultUpdateRequest) {
        if (scoring_resultUpdateRequest == null || scoring_resultUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        ScoringResult scoring_result = new ScoringResult();
        BeanUtils.copyProperties(scoring_resultUpdateRequest, scoring_result);
        // 数据校验
        scoringResultService.validScoringResult(scoring_result, false);
        // 判断是否存在
        long id = scoring_resultUpdateRequest.getId();
        ScoringResult oldScoringResult = scoringResultService.getById(id);
        ThrowUtils.throwIf(oldScoringResult == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = scoringResultService.updateById(scoring_result);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get/vo")
    public BaseResponse<ScoringResultVO> getScoringResultVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        ScoringResult scoring_result = scoringResultService.getById(id);
        ThrowUtils.throwIf(scoring_result == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(scoringResultService.getScoringResultVO(scoring_result, request));
    }
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ScoringResult>> listScoringResultByPage(@RequestBody ScoringResultQueryRequest scoring_resultQueryRequest) {
        long current = scoring_resultQueryRequest.getCurrent();
        long size = scoring_resultQueryRequest.getPageSize();
        // 查询数据库
        Page<ScoringResult> scoring_resultPage = scoringResultService.page(new Page<>(current, size),
                scoringResultService.getQueryWrapper(scoring_resultQueryRequest));
        return ResultUtils.success(scoring_resultPage);
    }
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<ScoringResultVO>> listScoringResultVOByPage(@RequestBody ScoringResultQueryRequest scoring_resultQueryRequest,
                                                                         HttpServletRequest request) {
        long current = scoring_resultQueryRequest.getCurrent();
        long size = scoring_resultQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<ScoringResult> scoring_resultPage = scoringResultService.page(new Page<>(current, size),
                scoringResultService.getQueryWrapper(scoring_resultQueryRequest));
        // 获取封装类
        return ResultUtils.success(scoringResultService.getScoringResultVOPage(scoring_resultPage, request));
    }

}
