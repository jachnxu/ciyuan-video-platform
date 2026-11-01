package com.ciyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_coins")
public class VideoCoin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long videoId;
    private Long userId;
    private Integer amount;
    private LocalDateTime createdAt;
}
