package com.ciyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long fromUserId;
    /** like / comment / follow / system */
    private String type;
    private Long videoId;
    private String content;
    private Integer isRead;
    private LocalDateTime createdAt;
}
