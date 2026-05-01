package com.picture.backend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.picture.backend.config.CosClientConfig;
import com.picture.backend.exception.BusinessException;
import com.picture.backend.exception.ErrorCode;
import com.picture.backend.manager.CosManager;
import com.picture.backend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {  
  
    @Resource
    protected CosManager cosManager;
  
    @Resource  
    protected CosClientConfig cosClientConfig;
  
    /**  
     * 模板方法，定义上传流程  
     */  
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片  
        validPicture(inputSource);
  
        // 2. 图片上传地址  
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String suffix;
        if (!originFilename.contains(".jpg") && !originFilename.contains(".png")
                && !originFilename.contains(".jpeg") && !originFilename.contains(".webp")){
            suffix = "png";
        } else {
            suffix = FileUtil.getSuffix(originFilename);
            if (suffix.contains("jpeg") || suffix.contains("webp")) {
                suffix = suffix.substring(0,4);
            }
            else if (suffix.contains("jpg") || suffix.contains("png")) {
                suffix = suffix.substring(0,3);
            }

        }
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                suffix);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);  
  
        File file = null;  
        try {  
            // 3. 创建临时文件  
            //file = File.createTempFile(uploadPath, null);
            file = File.createTempFile(uploadFilename, null);
            // 处理文件来源（本地或 URL）
            processFile(inputSource, file);  
  
            // 4. 上传图片到对象存储
            // 得到上传后处理过的图片信息
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获得原图的信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            // 获得处理结果后的对象列表
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)){
                // 获得压缩图信息
                CIObject compressCiObject = objectList.get(0);
                CIObject thumbnailCiObject1 = compressCiObject;
                // 如果处理结果有多张图片，则获取缩略图
                if (objectList.size() > 1){
                    // 获得缩略图信息
                    thumbnailCiObject1 = objectList.get(1);
                }
                // 封装压缩图返回结果
                return buildResult(originFilename,uploadPath,compressCiObject,thumbnailCiObject1,imageInfo);
            }
            // 封装原图返回结果
            return buildResult(originFilename, file, uploadPath, imageInfo);  
        } catch (Exception e) {  
            log.error("图片上传到对象存储失败", e);  
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {  
            // 6. 清理临时文件  
            deleteTempFile(file);  
        }  
    }  
  
    /**  
     * 校验输入源（本地文件或 URL）  
     */  
    protected abstract void validPicture(Object inputSource);  
  
    /**  
     * 获取输入源的原始文件名  
     */  
    protected abstract String getOriginFilename(Object inputSource);  
  
    /**  
     * 处理输入源并生成本地临时文件  
     */  
    protected abstract void processFile(Object inputSource, File file) throws Exception;  
  
    /**  
     * 封装返回结果  
     */  
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {  
        UploadPictureResult uploadPictureResult = new UploadPictureResult();  
        int picWidth = imageInfo.getWidth();  
        int picHeight = imageInfo.getHeight();  
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));  
        uploadPictureResult.setPicWidth(picWidth);  
        uploadPictureResult.setPicHeight(picHeight);  
        uploadPictureResult.setPicScale(picScale);  
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        // 设置图片主色调
        uploadPictureResult.setPicColor(imageInfo.getAve());
        uploadPictureResult.setPicSize(FileUtil.size(file));  
        uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
        return uploadPictureResult;  
    }

    /**
     * 封装压缩图返回结果
     */
    private UploadPictureResult buildResult(String originFilename, String uploadPath, CIObject compressedCiObject, CIObject thumbnailCiObject, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        // 设置图片为压缩后的地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        // 设置缩略图的地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        // 设置原图的地址
        uploadPictureResult.setOriginUrl(cosClientConfig.getHost() + uploadPath);
        // 设置图片主色调
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

    /**  
     * 删除临时文件  
     */  
    public void deleteTempFile(File file) {  
        if (file == null) {  
            return;  
        }  
        boolean deleteResult = file.delete();  
        if (!deleteResult) {  
            log.error("file delete error, filepath = {}", file.getAbsolutePath());  
        }  
    }  
}
