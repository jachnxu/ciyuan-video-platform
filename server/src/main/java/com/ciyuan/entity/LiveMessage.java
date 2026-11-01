package com.ciyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_messages")
public class LiveMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long userId;
    private String content;
    /** 0 聊天 1 弹幕 2 系统 3 进入 */
    private Integer type;
    private String color;
    private LocalDateTime createdAt;
}
