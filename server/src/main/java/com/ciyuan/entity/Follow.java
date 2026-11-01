package com.ciyuan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("follows")
public class Follow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long followerId;
    private Long followingId;
    private LocalDateTime createdAt;
}
