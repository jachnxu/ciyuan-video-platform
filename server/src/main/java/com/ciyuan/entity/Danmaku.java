package com.ciyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("danmaku")
public class Danmaku {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long videoId;
    private Long userId;
    private String content;
    private Double timeSec;
    private String color;
    private LocalDateTime createdAt;
}
