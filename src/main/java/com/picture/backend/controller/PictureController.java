package com.picture.backend.controller;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.picture.backend.annotation.AuthCheck;
import com.picture.backend.api.aliyunai.AliYunAiApi;
import com.picture.backend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.picture.backend.api.aliyunai.model.CreateTextGenPictureRequest;
import com.picture.backend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.picture.backend.api.imagesearch.ImageSearchApiFacade;
import com.picture.backend.api.imagesearch.model.ImageSearchResult;
import com.picture.backend.auth.SpaceUserAuthManager;
import com.picture.backend.auth.StpKit;
import com.picture.backend.auth.annotation.SaSpaceCheckPermission;
import com.picture.backend.auth.model.SpaceUserPermissionConstant;
import com.picture.backend.common.BaseResponse;
import com.picture.backend.common.DeleteRequest;
import com.picture.backend.common.ResultUtils;
import com.picture.backend.constant.UserConstant;
import com.picture.backend.exception.BusinessException;
import com.picture.backend.exception.ErrorCode;
import com.picture.backend.manager.CacheManager;
import com.picture.backend.model.dto.picture.*;
import com.picture.backend.model.entity.Picture;
import com.picture.backend.model.entity.Space;
import com.picture.backend.model.entity.User;
import com.picture.backend.model.enums.PictureReviewStatusEnum;
import com.picture.backend.model.vo.PictureTagCategory;
import com.picture.backend.model.vo.PictureVO;
import com.picture.backend.service.PictureService;
import com.picture.backend.service.SpaceService;
import com.picture.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static com.picture.backend.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {
    @Resource
    private UserService userService;
    @Resource
    private CacheManager cacheManager;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        // 获取登录用户信息
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        cacheManager.batchDeleteCache("picture:listPictureVOByPage");
        return ResultUtils.success(pictureVO);
    }

    /**
     * 从公共图库收藏照片到其他空间
     * @param pictureCollectRequest
     * @param request
     */
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    @PostMapping("/collect")
    public BaseResponse<PictureVO> collectPictureToOtherFromPublic(@RequestBody PictureCollectRequest pictureCollectRequest,HttpServletRequest request) {
        if (pictureCollectRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO= pictureService.collectPictureToOtherFromPublic(pictureCollectRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过url上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        // 获取登录用户信息
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(pictureUploadRequest.getFileUrl(), pictureUploadRequest, loginUser);
        cacheManager.batchDeleteCache("picture:listPictureVOByPage:");
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        pictureService.deletePicture(deleteRequest, request);
        // 删除图片缓存，刷新缓存
        cacheManager.batchDeleteCache("picture:listPictureVOByPage");
        return ResultUtils.success(true);
    }


    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        if (oldPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        cacheManager.batchDeleteCache("picture:listPictureVOByPage:");
        return ResultUtils.success(true);
    }


    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询数据库
        Picture picture = pictureService.getById(id);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询数据库
        Picture picture = pictureService.getById(id);
        if (picture == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 空间的图片，需要校验权限
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            if (!hasPermission){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            space = spaceService.getById(spaceId);
            if (space == null){
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            }
        }
        // 获取当前登录用户
        User loginUser;
        Object userObject = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObject;
        if (currentUser == null || currentUser.getId() == null) {
            loginUser = null;
        }else{
            loginUser = currentUser;
        }
        // 获取权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }


    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 一页最多展示20条数据，限制爬虫
        if (size > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "1页最多展示20张图片");
        }
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) {
            // 普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            if (!hasPermission){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        String key = null;
        if (spaceId == null) {
            // 创建缓存key
            String jsonStr = JSONUtil.toJsonStr(pictureQueryRequest);
            String hashKey = DigestUtils.md5DigestAsHex(jsonStr.getBytes());
            key = "picture:listPictureVOByPage:" + hashKey;
            // 查询缓存
            Page<PictureVO> page = cacheManager.inspectMutiLevelCache(key, new TypeReference<Page<PictureVO>>() {
            });
            if (page != null) {
                return ResultUtils.success(page);
            }
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        // 私人空间图片不需要缓存
        if (spaceId == null) {
            // 创建缓存
            if (pictureVOPage.getRecords().isEmpty()) {
                // 空结果缓存时间短一些（10秒）
                cacheManager.createNullCache(key, pictureVOPage, 10);
            } else {
                // 正常数据缓存 6-10 分钟
                cacheManager.createMultiLevelCache(key, pictureVOPage);
            }
        }
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        pictureService.editPicture(pictureEditRequest, request);
        cacheManager.batchDeleteCache("picture:listPictureVOByPage:");
        return ResultUtils.success(true);
    }


    /**
     * 获取标签和分类
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList(
                "热门",
                "自然", "风景", "山川", "海洋", "日落", "星空",
                "动物", "猫咪", "狗狗", "鸟类", "野生动物",
                "人物", "肖像", "街拍", "旅行", "纪实",
                "美食", "甜品", "饮品", "烹饪", "食材",
                "建筑", "城市", "室内", "极简", "工业",
                "艺术", "插画", "水彩", "油画", "素描", "3D",
                "平面设计", "UI设计", "品牌", "字体", "图标",
                "科技", "编程", "数码", "人工智能", "汽车",
                "植物", "花卉", "多肉", "庭院", "森林",
                "节日", "生日", "圣诞", "新年", "婚礼",
                "情感", "治愈", "励志", "孤独", "温暖",
                "复古", "胶片", "黑白", "电影感", "光影",
                "抽象", "纹理", "图案", "色彩", "渐变",
                "教育", "办公", "医疗", "体育", "音乐"
        );
        List<String> categoryList = Arrays.asList(
                "壁纸", "头像", "背景",
                "模板", "海报", "banner", "封面",
                "电商", "素材", "表情包",
                "插画", "摄影", "设计", "UI",
                "教育", "科技", "美食", "旅行", "时尚"
        );
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 图片审核
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        if (pictureReviewRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        cacheManager.batchDeleteCache("picture:listPictureVOByPage:");
        return ResultUtils.success(true);
    }

    /**
     * 图片上传（批量）
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request) {
        if (pictureUploadByBatchRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        cacheManager.batchDeleteCache("picture:listPictureVOByPage:");
        return ResultUtils.success(uploadCount);
    }

    /**
     * 删除所有缓存
     */
    @PostMapping("/cache/delete/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> deleteAllCache() {
        String success = cacheManager.deleteAllCache();
        return ResultUtils.success(success);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        // 校验参数
        if (searchPictureByPictureRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 图片id不能为空
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        if (pictureId == null || pictureId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 拿到图片完整对象
        Picture oldPicture = pictureService.getById(pictureId);
        if (oldPicture == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getUrl());
        return ResultUtils.success(resultList);
    }

    /**
     * 根据颜色搜索图片
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        if (searchPictureByColorRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 批量编辑图片
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        if (pictureEditByBatchRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }

    /**
     * 创建 AI 生图任务
     */
    @PostMapping("/text_picture/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<String> createPictureFromTextTask(
            @RequestBody CreateTextGenPictureRequest textGenPictureRequest) {
        if (textGenPictureRequest == null || textGenPictureRequest.getPrompt() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String taskId = pictureService.TextGenPicture(textGenPictureRequest);
        return ResultUtils.success(taskId);
    }

    /**
     * 查询 AI 生图任务
     */
    @GetMapping("/text_picture/get_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<ImageSynthesisResult> getPictureFromTextTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ImageSynthesisResult result = aliYunAiApi.getTextGenPictureTask(taskId);
        return ResultUtils.success(result);
    }

}
