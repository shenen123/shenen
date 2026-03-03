package com.liubinrui.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.liubinrui.annotation.AuthCheck;

import com.liubinrui.client.QwenClient;
import com.liubinrui.common.*;
import com.liubinrui.constant.UserConstant;
import com.liubinrui.exception.BusinessException;
import com.liubinrui.exception.ThrowUtils;

import com.liubinrui.manager.QuestionGenerationStrategyManager;
import com.liubinrui.model.dto.ai.AiGenerateQuestionRequest;
import com.liubinrui.model.dto.app.AppAddRequest;
import com.liubinrui.model.dto.app.AppEditRequest;
import com.liubinrui.model.dto.app.AppQueryRequest;
import com.liubinrui.model.dto.app.AppUpdateRequest;
import com.liubinrui.model.entity.App;

import com.liubinrui.model.entity.QuestionContent;
import com.liubinrui.model.entity.User;

import com.liubinrui.model.vo.AppVO;
import com.liubinrui.service.AppService;
import com.liubinrui.service.UserService;
import com.liubinrui.strategy.QuestionGenerationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/app")
@Slf4j
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Value("${ai.dashscope.api-key}")
    private String dashscopeApiKey;
    @Resource
    private QuestionGenerationStrategyManager strategyManager;

    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        App app = new App();
        BeanUtils.copyProperties(appAddRequest, app);
        // 数据校验
        appService.validApp(app, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        app.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newAppId = app.getId();
        return ResultUtils.success(newAppId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(user.getId()) && !userService.isAdminRequest(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = appService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest) {
        if (appUpdateRequest == null || appUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        App app = new App();
        BeanUtils.copyProperties(appUpdateRequest, app);
        // 数据校验
        appService.validApp(app, false);
        // 判断是否存在
        long id = appUpdateRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAppVO(app, request));
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<App>> listAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        // 查询数据库
        Page<App> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AppVO>> listAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                     HttpServletRequest request) {
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<App> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        // 获取封装类
        return ResultUtils.success(appService.getAppVOPage(appPage, request));
    }

    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        appQueryRequest.setUserId(loginUser.getId());
        long current = appQueryRequest.getCurrent();
        long size = appQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<App> appPage = appService.page(new Page<>(current, size),
                appService.getQueryWrapper(appQueryRequest));
        // 获取封装类
        return ResultUtils.success(appService.getAppVOPage(appPage, request));
    }

    @PostMapping("/ai_generate")
    public BaseResponse<List<QuestionContent>> aiGenerateQuestion(
            @RequestBody AiGenerateQuestionRequest aiGenerateQuestionRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.NOT_FOUND_ERROR);
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        App app = appService.getById(appId);
        ThrowUtils.throwIf(appId == null || app == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(ObjUtil.isNull(questionNumber) || ObjUtil.isNull(optionNumber), ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_FOUND_ERROR);
        QuestionGenerationStrategy strategy = strategyManager.getStrategyByAppType(app.getAppType());
        String systemMessage = strategy.getSystemMessage(questionNumber, optionNumber);
        String userMessage = strategy.getUserMessage(app, questionNumber, optionNumber);
        QwenClient qwenClient = new QwenClient();
        String result = null;
        try {
            result = qwenClient.chat(systemMessage, userMessage, dashscopeApiKey);

        } catch (Exception e) {
            log.error("调用 Qwen API 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 服务调用失败，请稍后重试");
        }
        log.info("AI 原始返回: {}", result); // 用于排查问题
        //[
        //  {
        //    "id": 1,
        //    "name": "张三",
        //    "age": 10
        //  },
        //  {
        //    "id": 2,
        //    "name": "李四",
        //    "age": 12
        //  }
        //]
        // 清洗 AI 响应，提取合法JSON数组
        JSONArray jsonArray = extractJsonArray(result);
        // ====== 转换为 DTO 列表 ======
        List<QuestionContent> questionContentDTOList = jsonArray.toList(QuestionContent.class);
        // 保存题目
        appService.insertQuestion(appId, loginUser, questionContentDTOList);
        return ResultUtils.success(questionContentDTOList);
    }

    @NotNull
    private static JSONArray extractJsonArray(String result) {

        // 1. 移除所有控制字符（U+0000 到 U+001F），这些字符会破坏 JSON 解析
        String cleanedResult = result.replaceAll("[\\x00-\\x1F]", "");

        // 2. 精准提取最外层的 [...]
        int start = -1;
        int end = -1;
        int depth = 0;
        for (int i = 0; i < cleanedResult.length(); i++) {
            char c = cleanedResult.charAt(i);
            if (c == '[') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0 && start != -1) {
                    end = i;
                    break;
                }
            }
        }

        if (start == -1 || end == -1) {
            log.error("无法提取 JSON 数组，清洗后内容: {}", cleanedResult);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回结果格式错误，未找到有效 JSON 数组");
        }

        String cleanJson = cleanedResult.substring(start, end + 1);

        // 3. 尝试解析
        JSONArray jsonArray;
        try {
            jsonArray = JSONUtil.parseArray(cleanJson);
        } catch (Exception e) {
            log.error("JSON 解析失败，清洗后内容: {}", cleanJson, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回的 JSON 格式非法");
        }
        return jsonArray;
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editApp(@RequestBody AppEditRequest appEditRequest, HttpServletRequest request) {
        if (appEditRequest == null || appEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        App app = new App();
        BeanUtils.copyProperties(appEditRequest, app);
        // 数据校验
        appService.validApp(app, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = appEditRequest.getId();
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldApp.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
