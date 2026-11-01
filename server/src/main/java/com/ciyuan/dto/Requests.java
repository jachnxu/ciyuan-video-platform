package com.ciyuan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class Requests {

    @Data
    public static class Register {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
        private String username;
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度 6-32 位")
        private String password;
        private String nickname;
    }

    @Data
    public static class Login {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class CommentReq {
        private Long videoId;
        private Long rootId;
        private Long parentId;
        private Long replyUserId;
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 500, message = "评论最多 500 字")
        private String content;
    }

    @Data
    public static class DanmakuReq {
        private Long videoId;
        @NotBlank(message = "弹幕内容不能为空")
        @Size(max = 60, message = "弹幕最多 60 字")
        private String content;
        private Double time;
        private String color;
    }

    @Data
    public static class ProfileReq {
        private String nickname;
        private String sign;
        private String avatar;
        private Integer gender;
    }

    @Data
    public static class PasswordReq {
        @NotBlank(message = "请输入原密码")
        private String oldPassword;
        @NotBlank(message = "请输入新密码")
        @Size(min = 6, max = 32, message = "新密码长度 6-32 位")
        private String newPassword;
    }

    @Data
    public static class Publish {
        @NotBlank(message = "标题不能为空")
        @Size(max = 80, message = "标题最多 80 字")
        private String title;
        @Size(max = 2000, message = "简介最多 2000 字")
        private String description;
        private Long categoryId;
        private String tags;
    }
}
