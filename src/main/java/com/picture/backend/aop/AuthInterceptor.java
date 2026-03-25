package com.picture.backend.aop;

import com.picture.backend.annotation.AuthCheck;
import com.picture.backend.exception.BusinessException;
import com.picture.backend.exception.ErrorCode;
import com.picture.backend.model.entity.User;
import com.picture.backend.model.enums.UserRoleEnums;
import com.picture.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@Slf4j
public class AuthInterceptor {
    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取访问这个接口所需要的权限
        String userRole = authCheck.mustRole();
        // 接口的访问权限转回枚举类
        UserRoleEnums roleEnums = UserRoleEnums.getEnumByValue(userRole);
        // 1、调用接口不需要权限的情况
        // 如果接口不需要权限就直接放行
        if (roleEnums == null){
            return joinPoint.proceed();
        }
        // 2、调用接口需要权限的情况
        // 获取当前请求的所有属性
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        // 获取servlet请求
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = servletRequestAttributes.getRequest();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnums localUserRoleEnum = UserRoleEnums.getEnumByValue(loginUser.getUserRole());
        // 如果用户没有登录
        if (localUserRoleEnum == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 如果接口访问要求有管理员权限，而用户没有管理员权限
        if(UserRoleEnums.ADMIN.equals(roleEnums) && !localUserRoleEnum.equals(UserRoleEnums.ADMIN)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 其余情况放行
        return joinPoint.proceed();
    }
}
