package com.ciyuan.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;
        Login login = hm.getMethodAnnotation(Login.class);
        if (login == null) login = hm.getBeanType().getAnnotation(Login.class);
        if (login == null) return true;
        if (UserContext.id() == null) throw BizException.unauthorized();
        if (login.admin() && !UserContext.isAdmin()) throw BizException.forbidden();
        return true;
    }
}
