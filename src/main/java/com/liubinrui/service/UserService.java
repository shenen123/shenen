package com.liubinrui.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.liubinrui.model.dto.user.UserQueryRequest;
import com.liubinrui.model.entity.User;
import com.liubinrui.model.vo.LoginUserVO;
import com.liubinrui.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);
    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);
    /**
     * 校验是否合法用户
     *
     * @param user
     * @param add 对创建的数据进行校验
     */
    void validUser(User user, boolean add);
    /**
     * 用户退出登录
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);
    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
    
    /**
     * 获取用户封装
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);
    /**
     * 获取脱敏的用户信息列表
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);
    /**
     * 分页获取用户封装
     *
     * @param userPage
     * @param request
     * @return
     */
    Page<UserVO> getUserVOPage(Page<User> userPage, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    boolean isAdminRequest(HttpServletRequest request);

    boolean isAdmin(User loginUser);
}
