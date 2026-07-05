package com.example.javabigwork.auth;

import com.example.javabigwork.auth.entity.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;
    private final UserContext userContext;

    public AuthInterceptor(AuthService authService, UserContext userContext) {
        this.authService = authService;
        this.userContext = userContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            UserAccount user = authService.loadFromSession(request);
            if (user != null) {
                userContext.set(user);
            }
            return true;
        }

        UserAccount user = authService.loadFromSession(request);
        if (user == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, "请先登录");
            return false;
        }
        userContext.set(user);

        if (requiresAdmin(path) && !"ADMIN".equals(user.role)) {
            writeError(response, HttpStatus.FORBIDDEN, "需要管理员权限");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        userContext.clear();
    }

    private boolean requiresAdmin(String path) {
        return path.startsWith("/api/admin/")
                || path.equals("/api/knowledge")
                || path.startsWith("/api/knowledge/");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }
}
