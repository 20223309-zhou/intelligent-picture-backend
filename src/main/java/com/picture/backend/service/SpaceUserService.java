package com.picture.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.picture.backend.model.dto.spaceuser.SpaceUserAddRequest;
import com.picture.backend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.picture.backend.model.entity.SpaceUser;
import com.picture.backend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Administrator
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2026-04-05 15:40:37
*/
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 添加空间
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) throws InterruptedException;

    /**
     * 获取空间查询条件
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 空间实体类转换成VO
     *
     * @param spaceUser
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * space集合转换成VO集合
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserPage);

    /**
     * 校验空间合法性
     *
     * @param spaceUser
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

}
