package com.javaplatform.api;

import com.javaplatform.service.*;
import com.javaplatform.util.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin("*")
public class ChatController {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    /**
     * Get chat history for user
     */
    @GetMapping("/history/{userId}")
    public Map<String, Object> getChatHistory(
        @PathVariable Long userId,
        @RequestHeader("Authorization") String authHeader) {
        
        try {
            var messages = messageService.getUserMessages(userId);
            var count = messageService.getUserMessageCount(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages);
            response.put("totalMessages", count);
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    /**
     * Get latest messages
     */
    @GetMapping("/latest")
    public Map<String, Object> getLatestMessages(
        @RequestParam(defaultValue = "50") int limit,
        @RequestHeader("Authorization") String authHeader) {
        
        try {
            var messages = messageService.getLatestMessages(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages);
            response.put("count", messages.size());
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    /**
     * Send message
     */
    @PostMapping("/send")
    public Map<String, Object> sendMessage(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody Map<String, String> request) {
        
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            String content = request.get("content");
            
            if (content == null || content.trim().isEmpty()) {
                return errorResponse("Message content is required");
            }
            
            var message = messageService.saveMessage(
                Long.parseLong(userId), 
                content
            ).get(); // Wait for async result
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message sent");
            response.put("messageId", message.getId());
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    /**
     * Get session status
     */
    @GetMapping("/session/{sessionId}")
    public Map<String, Object> getSessionStatus(
        @PathVariable String sessionId,
        @RequestHeader("Authorization") String authHeader) {
        
        try {
            var session = chatService.getSession(sessionId);
            
            Map<String, Object> response = new HashMap<>();
            if (session != null) {
                response.put("success", true);
                response.put("sessionId", session.getSessionId());
                response.put("userName", session.getUserName());
                response.put("messageCount", session.getMessages().size());
                response.put("createdAt", session.getCreatedAt());
            } else {
                response.put("success", false);
                response.put("message", "Session not found");
            }
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    /**
     * Get active sessions count
     */
    @GetMapping("/stats")
    public Map<String, Object> getChatStats(@RequestHeader("Authorization") String authHeader) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activeSessions", chatService.getActiveSessionCount());
            
            return response;
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
    
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
}
