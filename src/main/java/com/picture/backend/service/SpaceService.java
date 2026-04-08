package com.picture.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.picture.backend.model.dto.space.SpaceAddRequest;
import com.picture.backend.model.dto.space.SpaceQueryRequest;
import com.picture.backend.model.dto.user.UserQueryRequest;
import com.picture.backend.model.entity.Space;
import com.picture.backend.model.entity.User;
import com.picture.backend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author Administrator
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-03-31 16:47:58
*/
public interface SpaceService extends IService<Space> {
    /**
     * 添加空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) throws InterruptedException;

    /**
     * 获取空间查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 空间实体类转换成VO
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * space集合转换成VO集合
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 校验空间合法性
     *
     * @param space
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间等级填充空间信息
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 权限校验
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
