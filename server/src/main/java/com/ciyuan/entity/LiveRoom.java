package com.ciyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_rooms")
public class LiveRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String cover;
    private Long categoryId;
    private String streamKey;
    /** 0 未开播 1 直播中 2 已结束 */
    private Integer status;
    private Long viewers;
    private Long peakViewers;
    private Long totalViewers;
    private Long likeCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
