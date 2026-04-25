package com.picture.backend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureCollectRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long id;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}

