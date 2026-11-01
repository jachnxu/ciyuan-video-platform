package com.ciyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("videos")
public class Video {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long categoryId;
    private String title;
    private String description;
    private String cover;
    private String videoUrl;
    private Integer duration;
    private Integer width;
    private Integer height;
    /** 0 转码中 1 已发布 2 已下架 */
    private Integer status;
    private Long views;
    private Long danmakuCount;
    private Long likeCount;
    private Long coinCount;
    private Long favCount;
    private Long commentCount;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
