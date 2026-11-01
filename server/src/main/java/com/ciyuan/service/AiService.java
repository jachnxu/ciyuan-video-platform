package com.ciyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciyuan.dto.AiRequests;
import com.ciyuan.entity.Category;
import com.ciyuan.entity.Video;
import com.ciyuan.mapper.CategoryMapper;
import com.ciyuan.mapper.VideoMapper;
import com.ciyuan.vo.AiVos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 大模型视频推荐。
 *
 * <p>核心思路：不让模型凭空生成视频信息，而是先按用户提问从库里检索候选视频，
 * 把候选清单塞进 prompt，只让模型「从清单里挑 id + 写理由」，服务端再拿 id 回查数据库。
 * 这样模型永远不可能推荐出不存在的视频（防幻觉）。</p>
 *
 * <p>任何环节失败（未配置 Key / 网络异常 / 模型输出不是 JSON）都会降级为
 * 「按站内热度推荐」，保证接口始终有结果返回。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final VideoMapper videoMapper;
    private final CategoryMapper categoryMapper;
    private final VideoService videoService;
    private final ObjectMapper objectMapper;

    @Value("${ciyuan.ai.api-key:}")
    private String apiKey;

    @Value("${ciyuan.ai.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ciyuan.ai.model:deepseek-chat}")
    private String model;

    @Value("${ciyuan.ai.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${ciyuan.ai.max-candidates:24}")
    private int maxCandidates;

    /** 提问里的口语化虚词，检索前先剔除，避免污染关键词 */
    private static final String[] STOP_WORDS = {
            "我想看", "想看", "我要看", "我想找", "帮我看", "帮我找", "帮我", "给我",
            "推荐一下", "推荐点", "推荐些", "推荐几个", "推荐", "有没有", "有哪些", "有什么",
            "找点", "找找", "来点", "来几个", "几个", "一些", "一点", "那种", "这种",
            "可以", "能不能", "什么", "怎么", "视频", "吧", "吗", "呢", "的", "了", "和", "或者"
    };

    private static final String SYSTEM_PROMPT = String.join("\n",
            "你是「次元站」的视频推荐助手，服务对象是来看视频的用户。",
            "",
            "规则：",
            "1. 你只能从【候选视频】列表里挑选，绝对不允许编造列表中不存在的视频、标题、UP 主或播放量。",
            "2. 挑选 3~6 个最符合用户需求的视频，按匹配度从高到低排列；如果用户需求很宽泛，就挑质量最高的几个。",
            "3. 每个视频给一句推荐理由，20 字以内，要具体、口语化，说清楚「为什么适合他」，不要写空话套话。",
            "4. 如果候选里确实没有合适的，就在 reply 里诚实说明，然后挑最接近的 2~3 个作为备选。",
            "5. reply 是你的自然语言回答，60~150 字，友好简洁，可以提到你挑了几个、为什么这么挑。",
            "6. 不要说「根据候选列表」「以下是」这类机械措辞，像朋友推荐一样自然。",
            "",
            "只输出 JSON，不要输出任何其他内容，格式：",
            "{\"reply\":\"你的回答\",\"picks\":[{\"id\":12,\"reason\":\"推荐理由\"}]}"
    );

    // ------------------------------------------------------------------ 对外

    /** 一次检索的结果：matched 是真正命中用户需求的，all 是补齐后的完整候选集 */
    private static class Retrieval {
        List<Video> matched = new ArrayList<>();
        List<Video> all = new ArrayList<>();
    }

    public boolean configured() {
        return StringUtils.hasText(apiKey);
    }

    public String modelName() {
        return model;
    }

    public AiVos.ChatResult chat(AiRequests.Chat req) {
        return chatInternal(req, SYSTEM_PROMPT);
    }

    private AiVos.ChatResult chatInternal(AiRequests.Chat req, String systemPrompt) {
        String question = req.getMessage().trim();
        Retrieval rt = retrieve(question);

        if (rt.all.isEmpty()) {
            AiVos.ChatResult empty = new AiVos.ChatResult();
            empty.setReply("站里还没有可以推荐的视频，等有人投稿之后再来问我吧～");
            empty.setVideos(List.of());
            empty.setConfigured(configured());
            empty.setModel(model);
            empty.setCandidateCount(0);
            return empty;
        }

        if (!configured()) {
            return fallback(rt,
                    "AI 大脑还没接上（管理员还没配置密钥），先按站内热度给你挑了几个：");
        }

        try {
            String raw = callModel(question, req.getHistory(), rt.all, systemPrompt);
            return parseModelOutput(raw, rt);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            log.warn("AI 调用失败: {}", msg);
            String hint;
            if (msg.contains("401")) {
                hint = "AI 密钥无效（Key 填错或已失效），先按站内热度给你挑几个：";
            } else if (msg.contains("402")) {
                hint = "AI 账户余额不足，充值后即可恢复智能推荐。先按站内热度给你挑几个：";
            } else if (msg.contains("429")) {
                hint = "AI 调用太频繁被限流了，稍等一下再问我。先按热度给你挑几个：";
            } else {
                hint = "AI 暂时连不上，先按站内热度给你挑几个：";
            }
            return fallback(rt, hint);
        }
    }

    // ------------------------------------------------------------------ 检索

    /** 按提问检索候选视频：分区/关键词命中优先，不足则用热门补齐（补齐部分不参与降级展示） */
    private Retrieval retrieve(String question) {
        List<String> kws = keywords(question);
        Long catId = matchCategory(question);

        List<Video> matched;
        if (catId != null) {
            // 用户明确说了分区：先「分区 + 关键词」精确查，查不到就退到「分区内全部」
            matched = kws.isEmpty() ? List.of() : queryVideos(catId, kws);
            if (matched.isEmpty()) {
                matched = queryVideos(catId, null);
            }
        } else {
            matched = kws.isEmpty() ? List.of() : queryVideos(null, kws);
        }

        Retrieval rt = new Retrieval();
        rt.matched = matched;
        rt.all = matched;
        if (matched.size() >= 6) {
            return rt;
        }

        // 命中不足，用站内热门补齐，保证模型有足够素材可挑
        List<Video> hot = queryVideos(null, null);
        LinkedHashMap<Long, Video> merged = new LinkedHashMap<>();
        matched.forEach(v -> merged.put(v.getId(), v));
        hot.forEach(v -> merged.putIfAbsent(v.getId(), v));
        List<Video> all = new ArrayList<>(merged.values());
        if (all.size() > maxCandidates) {
            all = new ArrayList<>(all.subList(0, maxCandidates));
        }
        rt.all = all;
        return rt;
    }

    /** 统一查询入口：catId 与 kws 都可为 null，表示该维度不过滤 */
    private List<Video> queryVideos(Long catId, List<String> kws) {
        LambdaQueryWrapper<Video> w = new LambdaQueryWrapper<>();
        w.eq(Video::getStatus, 1);
        if (catId != null) {
            w.eq(Video::getCategoryId, catId);
        }
        if (kws != null && !kws.isEmpty()) {
            w.and(sub -> {
                for (int i = 0; i < kws.size(); i++) {
                    String k = kws.get(i);
                    if (i > 0) sub.or();
                    sub.like(Video::getTitle, k)
                            .or().like(Video::getTags, k)
                            .or().like(Video::getDescription, k);
                }
            });
        }
        w.orderByDesc(Video::getViews).orderByDesc(Video::getId);
        w.last("limit " + maxCandidates);
        return videoMapper.selectList(w);
    }

    /** 剔除口语虚词后切出检索关键词 */
    private List<String> keywords(String q) {
        String s = q;
        for (String w : STOP_WORDS) {
            s = s.replace(w, " ");
        }
        String[] parts = s.split("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (t.length() >= 2 && !out.contains(t)) {
                out.add(t);
            }
            if (out.size() >= 6) break;
        }
        if (out.isEmpty() && !q.isBlank()) {
            out.add(q.trim());
        }
        return out;
    }

    /** 提问里提到了哪个分区名，就收窄到该分区 */
    private Long matchCategory(String question) {
        List<Category> cats = categoryMapper.selectList(null);
        for (Category c : cats) {
            if (StringUtils.hasText(c.getName()) && question.contains(c.getName())) {
                return c.getId();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ 模型

    /** 推荐场景：带候选清单，要求模型输出 JSON */
    private String callModel(String question, List<AiVos.ChatMessage> history,
                             List<Video> candidates, String systemPrompt)
            throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                AiVos.ChatMessage h = history.get(i);
                if (h == null || !StringUtils.hasText(h.getContent())) continue;
                String role = "assistant".equals(h.getRole()) ? "assistant" : "user";
                messages.add(Map.of("role", role, "content", cut(h.getContent(), 300)));
            }
        }

        messages.add(Map.of("role", "user", "content",
                "【用户需求】" + question + "\n\n【候选视频】\n" + buildCandidatesText(candidates)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.6);
        body.put("max_tokens", 900);
        body.put("stream", false);
        body.put("response_format", Map.of("type", "json_object"));

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("模型 HTTP " + resp.statusCode() + " " + cut(resp.body(), 200));
        }
        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || !StringUtils.hasText(content.asText())) {
            throw new IllegalStateException("模型返回内容为空");
        }
        return content.asText();
    }

    private String buildCandidatesText(List<Video> list) {
        Map<Long, String> catNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        StringBuilder sb = new StringBuilder();
        for (Video v : list) {
            sb.append('[').append(v.getId()).append("] ")
                    .append(nvl(v.getTitle()))
                    .append(" | 分区:").append(catNames.getOrDefault(v.getCategoryId(), "未分类"))
                    .append(" | 标签:").append(StringUtils.hasText(v.getTags()) ? v.getTags() : "无")
                    .append(" | 时长:").append(fmtDuration(v.getDuration()))
                    .append(" | 播放:").append(v.getViews() == null ? 0 : v.getViews())
                    .append(" | 点赞:").append(v.getLikeCount() == null ? 0 : v.getLikeCount())
                    .append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ 解析

    private AiVos.ChatResult parseModelOutput(String raw, Retrieval rt) {
        JsonNode node = null;
        try {
            node = objectMapper.readTree(stripFence(raw));
        } catch (Exception e) {
            log.warn("模型输出不是合法 JSON，降级处理: {}", cut(raw, 120));
        }

        if (node == null || !node.isObject()) {
            return fallback(rt, "AI 这次回复格式有点乱，先按热度给你挑几个：");
        }

        Map<Long, Video> candMap = rt.all.stream()
                .collect(Collectors.toMap(Video::getId, v -> v, (a, b) -> a));

        List<AiVos.Recommend> out = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        JsonNode picks = node.path("picks");
        if (picks.isArray()) {
            for (JsonNode p : picks) {
                long id = p.path("id").asLong(0);
                String reason = p.path("reason").asText("");
                Video v = candMap.get(id);
                if (v == null) {
                    // 模型编了候选集之外的 id，直接丢弃
                    log.warn("丢弃候选集外的视频 id={}", id);
                    continue;
                }
                if (!used.add(id)) continue;
                AiVos.Recommend rec = new AiVos.Recommend();
                rec.setVideo(videoService.toVOs(List.of(v)).get(0));
                rec.setReason(StringUtils.hasText(reason) ? cut(reason, 60) : "和你的需求比较契合");
                out.add(rec);
                if (out.size() >= 6) break;
            }
        }

        String reply = node.path("reply").asText("");
        if (out.isEmpty()) {
            return fallback(rt, "AI 这次没挑出合适的，先按热度给你看看：");
        }

        AiVos.ChatResult r = new AiVos.ChatResult();
        r.setReply(StringUtils.hasText(reply)
                ? cut(reply, 600)
                : "按你的需求挑了 " + out.size() + " 个，看看合不合胃口～");
        r.setVideos(out);
        r.setConfigured(true);
        r.setModel(model);
        r.setCandidateCount(rt.all.size());
        return r;
    }

    /** 剥掉模型可能加的 ```json 围栏 */
    private String stripFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    // ------------------------------------------------------------------ 降级

    /** 模型不可用时：优先展示真正命中的视频（哪怕只有 1 个也不凑数），完全没命中才用热门 */
    private AiVos.ChatResult fallback(Retrieval rt, String reply) {
        List<Video> show = rt.matched.isEmpty() ? rt.all : rt.matched;
        List<AiVos.Recommend> recs = new ArrayList<>();
        for (Video v : show.stream().limit(4).toList()) {
            AiVos.Recommend rec = new AiVos.Recommend();
            rec.setVideo(videoService.toVOs(List.of(v)).get(0));
            long views = v.getViews() == null ? 0 : v.getViews();
            rec.setReason(views > 0 ? "站内热度较高，播放 " + views : "站内新投稿");
            recs.add(rec);
        }
        if (!rt.matched.isEmpty() && rt.matched.size() < 3) {
            reply = reply + "（站内相关视频目前只有 " + rt.matched.size() + " 个）";
        }
        AiVos.ChatResult r = new AiVos.ChatResult();
        r.setReply(reply);
        r.setVideos(recs);
        r.setConfigured(configured());
        r.setModel(model);
        r.setCandidateCount(rt.all.size());
        return r;
    }

    // ------------------------------------------------------------------ 工具

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String cut(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private String fmtDuration(Integer sec) {
        if (sec == null || sec <= 0) return "未知";
        int m = sec / 60;
        int s = sec % 60;
        if (m >= 60) return (m / 60) + "小时" + (m % 60) + "分";
        return s == 0 ? m + "分" : m + "分" + s + "秒";
    }
}
