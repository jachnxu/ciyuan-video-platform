package com.ciyuan.vo;

import lombok.Data;

import java.time.LocalDateTime;

public class LiveVos {

    @Data
    public static class LiveRoomVO {
        private Long id;
        private Long userId;
        private String title;
        private String cover;
        private Long categoryId;
        private String categoryName;
        private Integer status;
        private Long viewers;
        private Long peakViewers;
        private Long totalViewers;
        private Long likeCount;
        private String streamKey;
        private String pushUrl;
        private String playUrl;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private LocalDateTime createdAt;
        private Vos.UserVO author;
        private Boolean mine;
        private Boolean liked;
    }

    @Data
    public static class LiveMessageVO {
        private Long id;
        private Long roomId;
        private Long userId;
        private String content;
        private Integer type;
        private String color;
        private LocalDateTime createdAt;
        private Vos.UserVO user;
    }

    @Data
    public static class OnlineVO {
        private Long viewers;
    }
}
