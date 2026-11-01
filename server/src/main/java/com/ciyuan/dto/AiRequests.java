package com.ciyuan.dto;

import com.ciyuan.vo.AiVos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public class AiRequests {

    @Data
    public static class Chat {
        @NotBlank(message = "请输入你想看的内容")
        @Size(max = 500, message = "提问最多 500 字")
        private String message;

        /** 多轮对话历史，最多取最近 6 条 */
        private List<AiVos.ChatMessage> history;
    }
}
