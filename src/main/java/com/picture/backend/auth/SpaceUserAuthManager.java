package com.picture.backend.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.picture.backend.auth.model.SpaceUserAuthConfig;
import com.picture.backend.auth.model.SpaceUserRole;
import com.picture.backend.model.entity.Space;
import com.picture.backend.model.entity.SpaceUser;
import com.picture.backend.model.entity.User;
import com.picture.backend.model.enums.SpaceRoleEnum;
import com.picture.backend.model.enums.SpaceTypeEnum;
import com.picture.backend.service.SpaceUserService;
import com.picture.backend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class SpaceUserAuthManager {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return new ArrayList<>();
        }
        // 1.找到匹配的角色
        //1.1 获得角色列表集合
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                // 1.2 筛选出匹配的角色
                .filter(r -> spaceUserRole.equals(r.getKey()))
                // 1.3 获取角色
                .findFirst()
                // 1.4 没有找到则返回空
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        // 2.如果角色存在，则返回权限列表
        return role.getPermissions();
    }

    /**
     * 根据空间返回权限列表
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 获取管理员权限列表
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            // 如果访问公共图库且登录用户为管理员，则返回管理员权限
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            // 访问公共图库不为管理员返回 空集合
            return new ArrayList<>();
        }
        // 获取空间枚举类型
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        // 空间类型不存在返回空集合
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    // 返回角色权限列表
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

}
