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
import com.liubinrui.model.dto.user_answer.UserAnswerAddRequest;
import com.liubinrui.model.dto.user_answer.UserAnswerEditRequest;
import com.liubinrui.model.dto.user_answer.UserAnswerQueryRequest;
import com.liubinrui.model.dto.user_answer.UserAnswerUpdateRequest;
import com.liubinrui.model.entity.User;
import com.liubinrui.model.entity.UserAnswer;
import com.liubinrui.model.vo.UserAnswerVO;
import com.liubinrui.service.UserAnswerService;
import com.liubinrui.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题目接口

 */
@RestController
@RequestMapping("/user/answer")
@Slf4j
public class UserAnswerController {

    @Resource
    private UserAnswerService useranswerService;

    @Resource
    private UserService userService;

    @PostMapping("/add")
    public BaseResponse<Long> addUserAnswer(@RequestBody UserAnswerAddRequest useranswerAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(useranswerAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        UserAnswer useranswer = new UserAnswer();
        BeanUtils.copyProperties(useranswerAddRequest, useranswer);
        // 数据校验
        useranswerService.validUserAnswer(useranswer, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        useranswer.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = useranswerService.save(useranswer);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newUserAnswerId = useranswer.getId();
        return ResultUtils.success(newUserAnswerId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUserAnswer(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        UserAnswer oldUserAnswer = useranswerService.getById(id);
        ThrowUtils.throwIf(oldUserAnswer == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldUserAnswer.getUserId().equals(user.getId()) && !userService.isAdminRequest(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = useranswerService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUserAnswer(@RequestBody UserAnswerUpdateRequest useranswerUpdateRequest) {
        if (useranswerUpdateRequest == null || useranswerUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        UserAnswer useranswer = new UserAnswer();
        BeanUtils.copyProperties(useranswerUpdateRequest, useranswer);
        // 数据校验
        useranswerService.validUserAnswer(useranswer, false);
        // 判断是否存在
        long id = useranswerUpdateRequest.getId();
        UserAnswer oldUserAnswer = useranswerService.getById(id);
        ThrowUtils.throwIf(oldUserAnswer == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = useranswerService.updateById(useranswer);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/get/vo")
    public BaseResponse<UserAnswerVO> getUserAnswerVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        UserAnswer useranswer = useranswerService.getById(id);
        ThrowUtils.throwIf(useranswer == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(useranswerService.getUserAnswerVO(useranswer, request));
    }

    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserAnswer>> listUserAnswerByPage(@RequestBody UserAnswerQueryRequest useranswerQueryRequest) {
        long current = useranswerQueryRequest.getCurrent();
        long size = useranswerQueryRequest.getPageSize();
        // 查询数据库
        Page<UserAnswer> useranswerPage = useranswerService.page(new Page<>(current, size),
                useranswerService.getQueryWrapper(useranswerQueryRequest));
        return ResultUtils.success(useranswerPage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserAnswerVO>> listUserAnswerVOByPage(@RequestBody UserAnswerQueryRequest useranswerQueryRequest,
                                                               HttpServletRequest request) {
        long current = useranswerQueryRequest.getCurrent();
        long size = useranswerQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<UserAnswer> useranswerPage = useranswerService.page(new Page<>(current, size),
                useranswerService.getQueryWrapper(useranswerQueryRequest));
        // 获取封装类
        return ResultUtils.success(useranswerService.getUserAnswerVOPage(useranswerPage, request));
    }

    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<UserAnswerVO>> listMyUserAnswerVOByPage(@RequestBody UserAnswerQueryRequest useranswerQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(useranswerQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        useranswerQueryRequest.setUserId(loginUser.getId());
        long current = useranswerQueryRequest.getCurrent();
        long size = useranswerQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<UserAnswer> useranswerPage = useranswerService.page(new Page<>(current, size),
                useranswerService.getQueryWrapper(useranswerQueryRequest));
        // 获取封装类
        return ResultUtils.success(useranswerService.getUserAnswerVOPage(useranswerPage, request));
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editUserAnswer(@RequestBody UserAnswerEditRequest useranswerEditRequest, HttpServletRequest request) {
        if (useranswerEditRequest == null || useranswerEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserAnswer useranswer = new UserAnswer();
        BeanUtils.copyProperties(useranswerEditRequest, useranswer);
        // 数据校验
        useranswerService.validUserAnswer(useranswer, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = useranswerEditRequest.getId();
        UserAnswer oldUserAnswer = useranswerService.getById(id);
        ThrowUtils.throwIf(oldUserAnswer == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldUserAnswer.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = useranswerService.updateById(useranswer);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

}
