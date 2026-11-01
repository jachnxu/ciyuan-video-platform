package com.ciyuan.common;

public class UserContext {
    private static final ThreadLocal<Long> ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void set(Long id, String role) {
        ID.set(id);
        ROLE.set(role);
    }

    public static Long id() {
        return ID.get();
    }

    public static Long requireId() {
        Long id = ID.get();
        if (id == null) throw BizException.unauthorized();
        return id;
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(ROLE.get());
    }

    public static void clear() {
        ID.remove();
        ROLE.remove();
    }
}
