package com.picture.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.picture.backend.model.dto.UserQueryRequest;
import com.picture.backend.model.dto.UserRegisterRequest;
import com.picture.backend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.picture.backend.model.vo.LoginUserVO;
import com.picture.backend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-03-23 14:49:32
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 获取加密密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

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
     * 封装脱敏信息返回
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);


    /**
     * 获取脱敏信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏信息列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
