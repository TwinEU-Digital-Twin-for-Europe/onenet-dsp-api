package it.eng.onenet.dsp.api.security;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import it.eng.onenet.dsp.api.service.user.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    private final UserService userService;

    TokenInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String token = request.getHeader("authorization");

        if (token == null || !isValidToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or missing token");
            return false;
        }

        return true;
    }

    private boolean isValidToken(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", token);
        try {
            userService.getCurrentUser(headers);
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
