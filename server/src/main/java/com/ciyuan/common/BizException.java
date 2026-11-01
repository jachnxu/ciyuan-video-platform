package com.ciyuan.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String msg) {
        this(400, msg);
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public static BizException unauthorized() {
        return new BizException(401, "请先登录");
    }

    public static BizException forbidden() {
        return new BizException(403, "没有权限");
    }

    public static BizException notFound(String what) {
        return new BizException(404, what + "不存在");
    }
}
