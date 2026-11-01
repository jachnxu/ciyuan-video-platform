package com.ciyuan.vo;

import lombok.Data;

import java.util.List;

/**
 * 大模型推荐相关的出参结构
 */
public class AiVos {

    /** 对话历史中的一条消息 */
    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }

    /** 模型返回的挑选结果（只有 id 和理由，视频信息由服务端回查） */
    @Data
    public static class Pick {
        private Long id;
        private String reason;
    }

    /** 一条推荐：真实视频 + 推荐理由 */
    @Data
    public static class Recommend {
        private Vos.VideoVO video;
        private String reason;
    }

    /** 一次对话的完整结果 */
    @Data
    public static class ChatResult {
        /** 模型自然语言回复 */
        private String reply;
        /** 推荐视频列表 */
        private List<Recommend> videos;
        /** 服务端是否已配置 API Key */
        private boolean configured;
        /** 当前使用的模型名 */
        private String model;
        /** 本次检索到的候选视频数（便于排查推荐质量） */
        private int candidateCount;
    }
}
