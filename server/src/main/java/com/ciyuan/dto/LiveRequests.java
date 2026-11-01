package com.ciyuan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class LiveRequests {

    @Data
    public static class CreateRoom {
        @NotBlank(message = "标题不能为空")
        @Size(max = 120, message = "标题最多 120 字")
        private String title;
        private String cover;
        private Long categoryId;
    }

    @Data
    public static class MessageReq {
        @NotBlank(message = "内容不能为空")
        @Size(max = 200, message = "消息最多 200 字")
        private String content;
        private Integer type;
        private String color;
    }
}
