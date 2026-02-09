package com.liubinrui.service.impl;

import java.util.Date;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.liubinrui.model.vo.LoginUserVO;
import org.apache.commons.lang3.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.liubinrui.common.ErrorCode;
import com.liubinrui.constant.CommonConstant;
import com.liubinrui.constant.UserConstant;
import com.liubinrui.enums.UserRoleEnum;
import com.liubinrui.exception.BusinessException;
import com.liubinrui.exception.ThrowUtils;
import com.liubinrui.mapper.UserMapper;
import com.liubinrui.model.dto.user.UserQueryRequest;
import com.liubinrui.model.entity.User;
import com.liubinrui.model.vo.UserVO;
import com.liubinrui.service.UserService;
import com.liubinrui.util.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "liubinrui";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //检验非空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword))
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        //检验长度
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper).longValue();
        if (count > 0)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean result = this.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        queryWrapper.eq("user_password", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    private LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public void validUser(User user, boolean add) {

        String userAccount = user.getUserAccount();
        String userPassword = user.getUserPassword();
        String userRole = user.getUserRole();


        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR);
        // 创建数据时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(userAccount == null, ErrorCode.NOT_FOUND_ERROR);
            ThrowUtils.throwIf(userPassword == null, ErrorCode.NOT_FOUND_ERROR);
            ThrowUtils.throwIf(userRole == null, ErrorCode.NOT_FOUND_ERROR);
        }
        {

        }
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.NOT_FOUND_ERROR);
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "user_role", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "user_account", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "user_name", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "user_profile", userProfile);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        //orderBy(boolean condition, boolean isAsc, R column)
        return queryWrapper;
    }

    @Override
    public UserVO getUserVO(User user) {
        // 对象转封装类
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        UserVO userVO = UserVO.objToVo(user);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(user ->
                UserVO.objToVo(user)).collect(Collectors.toList());
    }

    @Override
    public Page<UserVO> getUserVOPage(Page<User> userPage, HttpServletRequest request) {
        //getRecords() 是 MyBatis-Plus Page 类的方法，返回当前页的记录列表
        List<User> userList = userPage.getRecords();
        //getCurrent()：当前页码（如第1页）
        //getSize()：每页大小（如10条/页）
        //getTotal()：总记录数（用于前端计算总页数）
        Page<UserVO> userVOPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        if (CollUtil.isEmpty(userList)) {
            return userVOPage;
        }
        // 对象列表 => 封装对象列表
        List<UserVO> userVOList = userList.stream().map(user -> UserVO.objToVo(user)).collect(Collectors.toList());
        //转换好的 userVOList 设置为 userVOPage 的记录列表
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public boolean isAdminRequest(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);

    }

    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

}
