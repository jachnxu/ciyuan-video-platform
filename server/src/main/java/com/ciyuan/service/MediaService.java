package com.ciyuan.service;

import com.ciyuan.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 本地文件存储 + ffmpeg 媒体处理
 */
@Slf4j
@Service
public class MediaService {

    @Value("${ciyuan.storage}")
    private String storage;

    @Value("${ciyuan.ffmpeg:ffmpeg}")
    private String ffmpeg;

    @Value("${ciyuan.ffprobe:ffprobe}")
    private String ffprobe;

    public Path dir(String sub) {
        Path p = Paths.get(storage, sub);
        try {
            Files.createDirectories(p);
        } catch (Exception e) {
            throw new BizException("无法创建存储目录: " + p);
        }
        return p;
    }

    public String save(MultipartFile file, String sub) {
        if (file == null || file.isEmpty()) throw new BizException("文件为空");
        String ext = extOf(file.getOriginalFilename());
        String name = UUID.randomUUID().toString().replace("-", "");
        Path target = dir(sub).resolve(name + (ext.isEmpty() ? "" : "." + ext));
        try {
            file.transferTo(target.toFile());
        } catch (Exception e) {
            throw new BizException("文件保存失败: " + e.getMessage());
        }
        return "/media/" + sub + "/" + target.getFileName();
    }

    public String extOf(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1).toLowerCase();
    }

    public void deleteByUrl(String url) {
        if (url == null || !url.startsWith("/media/")) return;
        try {
            Files.deleteIfExists(Paths.get(storage, url.substring("/media/".length())));
        } catch (Exception e) {
            log.warn("delete fail {}", url, e);
        }
    }

    public String pathOf(String url) {
        return Paths.get(storage, url.substring("/media/".length())).toString();
    }

    /** 返回 [duration秒, width, height, videoCodec, audioCodec]，失败返回 null */
    public String[] probe(String url) {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffprobe, "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=width,height,codec_name",
                    "-show_entries", "format=duration",
                    "-of", "default=nw=1:nk=1", pathOf(url));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.isBlank()) lines.add(line.trim());
                }
            }
            p.waitFor(20, TimeUnit.SECONDS);
            // 输出顺序: codec_name, width, height, duration
            String codec = lines.size() > 0 ? lines.get(0) : "";
            String w = lines.size() > 1 ? lines.get(1) : "0";
            String h = lines.size() > 2 ? lines.get(2) : "0";
            String dur = lines.size() > 3 ? lines.get(3) : "0";
            double d = 0;
            try {
                d = Double.parseDouble(dur);
            } catch (Exception ignore) {
            }
            String acodec = probeAudioCodec(pathOf(url));
            return new String[]{String.valueOf((int) Math.round(d)), w, h, codec, acodec};
        } catch (Exception e) {
            log.warn("ffprobe failed: {}", e.getMessage());
            return null;
        }
    }

    private String probeAudioCodec(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffprobe, "-v", "error", "-select_streams", "a:0",
                    "-show_entries", "stream=codec_name", "-of", "default=nw=1:nk=1", path);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            p.waitFor(15, TimeUnit.SECONDS);
            return out.isEmpty() ? "none" : out.lines().findFirst().orElse("none");
        } catch (Exception e) {
            return "none";
        }
    }

    /** 从视频第 sec 秒抽一帧作为封面 */
    public String generateCover(String videoUrl, int sec) {
        try {
            String name = UUID.randomUUID().toString().replace("-", "") + ".jpg";
            Path out = dir("cover").resolve(name);
            ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-y", "-ss", String.valueOf(Math.max(0, sec)),
                    "-i", pathOf(videoUrl), "-frames:v", "1", "-vf", "scale=854:-2", "-q:v", "3", out.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().readAllBytes();
            p.waitFor(60, TimeUnit.SECONDS);
            if (Files.exists(out) && Files.size(out) > 0) {
                return "/media/cover/" + name;
            }
        } catch (Exception e) {
            log.warn("cover generate failed: {}", e.getMessage());
        }
        return null;
    }

    /** 转码为浏览器友好的 h264 + aac mp4，返回新的 url */
    public String transcodeToMp4(String videoUrl) {
        String src = pathOf(videoUrl);
        String name = UUID.randomUUID().toString().replace("-", "") + ".mp4";
        Path out = dir("video").resolve(name);
        try {
            List<String> cmd = List.of(ffmpeg, "-y", "-i", src,
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "24",
                    "-vf", "scale='min(1280,iw)':-2",
                    "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart",
                    out.toString());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().readAllBytes();
            boolean done = p.waitFor(3, TimeUnit.HOURS);
            if (done && p.exitValue() == 0 && Files.exists(out) && Files.size(out) > 1024) {
                return "/media/video/" + name;
            }
            log.warn("transcode failed exit={}", done ? p.exitValue() : "timeout");
            Files.deleteIfExists(out);
            return null;
        } catch (Exception e) {
            log.warn("transcode error", e);
            return null;
        }
    }

    public boolean isBrowserFriendly(String vcodec, String acodec) {
        boolean v = "h264".equalsIgnoreCase(vcodec);
        boolean a = "aac".equalsIgnoreCase(acodec) || "mp3".equalsIgnoreCase(acodec) || "none".equalsIgnoreCase(acodec);
        return v && a;
    }

    public long sizeOf(String url) {
        try {
            return new File(pathOf(url)).length();
        } catch (Exception e) {
            return 0;
        }
    }
}
