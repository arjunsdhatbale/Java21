package com.main.notification;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String username = servletRequest.getServletRequest().getParameter("username");
            if (username == null || username.trim().isEmpty()) {
                username = servletRequest.getServletRequest().getParameter("userId");
            }
            if (username != null && !username.trim().isEmpty()) {
                return new StompPrincipal(username.trim());
            }
        }

        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && ("username".equalsIgnoreCase(pair[0]) || "userId".equalsIgnoreCase(pair[0]))) {
                    return new StompPrincipal(pair[1].trim());
                }
            }
        }

        return new StompPrincipal("anonymous-" + UUID.randomUUID().toString().substring(0, 8));
    }
}
