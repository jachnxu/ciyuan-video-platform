package com.ciyuan.service;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 浏览器摄像头推流 -> ffmpeg 转码 -> 本机 RTMP -> nginx-rtmp 转 HLS
 */
@Slf4j
class FfmpegStream {

    private final String key;
    private final String fmt;
    private Process process;
    private OutputStream stdin;
    private volatile boolean stopped = false;

    FfmpegStream(String key, String fmt) {
        this.key = key;
        this.fmt = (fmt == null || fmt.isBlank()) ? "webm" : fmt.toLowerCase();
    }

    synchronized void start() {
        try {
            String inputFmt = "mp4".equals(fmt) ? "mp4" : "webm";
            String rtmp = "rtmp://127.0.0.1:1935/live/" + key;
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-hide_banner", "-loglevel", "warning",
                    "-analyzeduration", "1000000", "-probesize", "1000000",
                    "-f", inputFmt, "-i", "pipe:0",
                    "-c:v", "libx264", "-preset", "veryfast", "-tune", "zerolatency",
                    "-pix_fmt", "yuv420p", "-g", "50",
                    "-b:v", "1500k", "-maxrate", "1800k", "-bufsize", "3000k",
                    "-c:a", "aac", "-ar", "44100", "-b:a", "128k",
                    "-f", "flv", rtmp
            );
            pb.redirectErrorStream(true);
            process = pb.start();
            stdin = process.getOutputStream();

            Thread drain = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        log.debug("[ffmpeg {}] {}", key, line);
                    }
                } catch (Exception ignored) {
                }
            }, "ffmpeg-drain-" + key);
            drain.setDaemon(true);
            drain.start();

            log.info("ffmpeg started for streamKey={} fmt={}", key, fmt);
        } catch (Exception e) {
            log.error("ffmpeg start failed for " + key, e);
        }
    }

    synchronized void write(byte[] data) {
        if (stopped || stdin == null) return;
        try {
            stdin.write(data);
            stdin.flush();
        } catch (Exception e) {
            log.warn("ffmpeg write failed for {}: {}", key, e.getMessage());
        }
    }

    synchronized void stop() {
        if (stopped) return;
        stopped = true;
        try {
            if (stdin != null) stdin.close();
        } catch (Exception ignored) {
        }
        try {
            if (process != null) {
                process.destroy();
                if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly();
            }
        } catch (Exception ignored) {
        }
        log.info("ffmpeg stopped for streamKey={}", key);
    }
}
