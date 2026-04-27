package com.picture.backend.api.aliyunai.model;

import lombok.Data;

import java.io.Serializable;


@Data
public class CreateTextGenPictureRequest implements Serializable {
    // 生图提示词
    private String prompt;
    // 是否使用扩展提示词
    private boolean promptExtend;
    // 是否使用水印
    private boolean watermark;
    // 图片尺寸
    private String size;
}
