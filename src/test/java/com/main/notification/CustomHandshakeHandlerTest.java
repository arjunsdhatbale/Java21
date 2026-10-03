package com.main.notification;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CustomHandshakeHandlerTest {

    private final CustomHandshakeHandler handshakeHandler = new CustomHandshakeHandler();
    private final WebSocketHandler wsHandler = mock(WebSocketHandler.class);

    @Test
    void testDetermineUser_withUsernameQueryParam() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws");
        servletRequest.setParameter("username", "arjun_dev");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);

        Principal principal = handshakeHandler.determineUser(request, wsHandler, new HashMap<>());

        assertNotNull(principal);
        assertEquals("arjun_dev", principal.getName());
    }

    @Test
    void testDetermineUser_withUserIdQueryParam() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws");
        servletRequest.setParameter("userId", "user_99");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);

        Principal principal = handshakeHandler.determineUser(request, wsHandler, new HashMap<>());

        assertNotNull(principal);
        assertEquals("user_99", principal.getName());
    }

    @Test
    void testDetermineUser_withoutQueryParam_assignsAnonymous() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);

        Principal principal = handshakeHandler.determineUser(request, wsHandler, new HashMap<>());

        assertNotNull(principal);
        assertTrue(principal.getName().startsWith("anonymous-"));
    }
}
