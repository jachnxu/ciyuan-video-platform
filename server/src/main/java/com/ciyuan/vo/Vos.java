package com.ciyuan.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Vos {

    @Data
    public static class UserVO {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private String sign;
        private Integer gender;
        private Integer level;
        private Integer coin;
        private String role;
        private LocalDateTime createdAt;
        private Long followers;
        private Long following;
        private Long videoCount;
        private Long totalViews;
        private Boolean followed;
    }

    @Data
    public static class VideoVO {
        private Long id;
        private String title;
        private String description;
        private String cover;
        private String videoUrl;
        private Integer duration;
        private Integer width;
        private Integer height;
        private Integer status;
        private Long views;
        private Long danmakuCount;
        private Long likeCount;
        private Long coinCount;
        private Long favCount;
        private Long commentCount;
        private String tags;
        private LocalDateTime createdAt;
        private Long categoryId;
        private String categoryName;
        private UserVO author;
        private Boolean liked;
        private Boolean coined;
        private Boolean faved;
        private Boolean mine;
    }

    @Data
    public static class CommentVO {
        private Long id;
        private String content;
        private LocalDateTime createdAt;
        private Long likeCount;
        private Boolean liked;
        private UserVO user;
        private UserVO replyUser;
        private List<CommentVO> children = new ArrayList<>();
        private Integer childCount;
    }

    @Data
    public static class NotifVO {
        private Long id;
        private String type;
        private String content;
        private Integer isRead;
        private LocalDateTime createdAt;
        private UserVO fromUser;
        private Long videoId;
        private String videoTitle;
        private String videoCover;
    }

    @Data
    public static class LoginResult {
        private String token;
        private UserVO user;
    }

    @Data
    public static class ToggleResult {
        private Boolean active;
        private Long count;
        private String msg;
    }

    @Data
    public static class Stats {
        private Long users;
        private Long videos;
        private Long views;
        private Long comments;
        private Long danmaku;
        private Long todayVideos;
        private Long pendingVideos;
        private Long todayUsers;
    }
}
