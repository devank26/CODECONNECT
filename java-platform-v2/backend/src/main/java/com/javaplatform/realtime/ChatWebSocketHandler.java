package com.javaplatform.realtime;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userMapping = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("WebSocket connection established: {}", session.getId());
        
        // Send connected users list
        broadcastOnlineUsers();
    }
    
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        String[] parts = payload.split("\\|", 2);
        
        if (parts.length < 2) {
            return;
        }
        
        String type = parts[0];
        String data = parts[1];
        
        if ("USER".equals(type)) {
            // Register user
            userMapping.put(session.getId(), data);
            broadcastOnlineUsers();
        } else if ("CHAT".equals(type)) {
            // Broadcast chat message
            String sender = userMapping.get(session.getId());
            String messageText = String.format("%s: %s", sender != null ? sender : "Anonymous", data);
            broadcastMessage(messageText);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        userMapping.remove(session.getId());
        log.info("WebSocket connection closed: {}", session.getId());
        broadcastOnlineUsers();
    }
    
    private void broadcastMessage(String message) throws IOException {
        TextMessage textMessage = new TextMessage("MSG|" + message);
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }
    
    private void broadcastOnlineUsers() throws IOException {
        List<String> users = new ArrayList<>(userMapping.values());
        String userList = String.join(",", users);
        TextMessage message = new TextMessage("USERS|" + userList);
        
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        }
    }
}
