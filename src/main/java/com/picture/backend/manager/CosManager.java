package com.picture.backend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.picture.backend.config.CosClientConfig;
import com.picture.backend.exception.BusinessException;
import com.picture.backend.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@Slf4j
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传用户头像
     */
    public String putUserAvatar(Long userId, MultipartFile  multipartFile) {
        if (multipartFile == null){
            return null;
        }
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L;
        if (fileSize > 2 * ONE_M){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        }
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        if (!ALLOW_FORMAT_LIST.contains(fileSuffix)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
        }
        // 3.获取源文件后缀
        String originFilename = multipartFile.getOriginalFilename();
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
        // 4.拼接上传路径
        String randomString = RandomUtil.randomString(16);
        String fileName = String.format("%s_%s.%s",userId.toString() ,randomString, suffix);
        String uploadPath = String.format("%s/%s", "avatar", fileName);
        // 5.创建临时文件
        File file = null;
        PutObjectRequest putObjectRequest;
        try {
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), uploadPath,
                    file);
            cosClient.putObject(putObjectRequest);
        } catch (IOException e) {
            log.info("头像上传失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
        }finally {
            // 6.清理临时文件
            deleteTempFile(file);
        }
        return cosClientConfig.getHost()  + "/" + uploadPath;
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


    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        // 创建规则
        PicOperations.Rule compressRule = new PicOperations.Rule();
        // 设置压缩图规则，设置存储桶
        //compressRule.setRule("imageMogr2/format/webp/thumbnail/512x512^");
        compressRule.setRule("imageMogr2/thumbnail/!512x512r/min/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        // 设置压缩图文件名
        compressRule.setFileId(webpKey);
        rules.add(compressRule);
        // 缩略图处理，仅对 > 20 KB 的图片生成缩略图
        if (file.length() > 20 * 1024) {
            // 创建缩略图规则
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }
        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 文件 key
     */
    public void deleteObject(String key) throws CosClientException {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }


}
