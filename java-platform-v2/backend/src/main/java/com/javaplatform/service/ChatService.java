package com.javaplatform.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatService {
    
    // Store active chat sessions
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();
    
    /**
     * Create new chat session
     */
    public ChatSession createSession(String sessionId, String userName) {
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserName(userName);
        session.setCreatedAt(System.currentTimeMillis());
        session.setMessages(new ArrayList<>());
        
        sessions.put(sessionId, session);
        log.info("Chat session created: {} for user: {}", sessionId, userName);
        
        return session;
    }
    
    /**
     * Get chat session
     */
    public ChatSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * Add message to session (in-memory buffer)
     */
    public void addMessageToSession(String sessionId, String sender, String content) {
        ChatSession session = sessions.get(sessionId);
        if (session != null) {
            ChatMessage message = new ChatMessage();
            message.setSender(sender);
            message.setContent(content);
            message.setTimestamp(System.currentTimeMillis());
            
            session.getMessages().add(message);
            
            // Keep only last 100 messages in memory
            if (session.getMessages().size() > 100) {
                session.getMessages().remove(0);
            }
        }
    }
    
    /**
     * Get session chat history
     */
    public List<ChatMessage> getChatHistory(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session != null) {
            return new ArrayList<>(session.getMessages());
        }
        return Collections.emptyList();
    }
    
    /**
     * End chat session
     */
    public void endSession(String sessionId) {
        ChatSession session = sessions.remove(sessionId);
        if (session != null) {
            log.info("Chat session ended: {} (messages: {})", 
                     sessionId, session.getMessages().size());
        }
    }
    
    /**
     * Get all active sessions
     */
    public Collection<ChatSession> getActiveSessions() {
        return sessions.values();
    }
    
    /**
     * Get active session count
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    // Inner class for chat session
    public static class ChatSession {
        private String sessionId;
        private String userName;
        private long createdAt;
        private List<ChatMessage> messages;
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public List<ChatMessage> getMessages() { return messages; }
        public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    }
    
    // Inner class for chat message
    public static class ChatMessage {
        private String sender;
        private String content;
        private long timestamp;
        
        // Getters and setters
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
