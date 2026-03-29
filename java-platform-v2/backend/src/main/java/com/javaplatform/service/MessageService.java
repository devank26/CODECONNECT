package com.javaplatform.service;

import com.javaplatform.model.Message;
import com.javaplatform.model.User;
import com.javaplatform.repository.MessageRepository;
import com.javaplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class MessageService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Save message asynchronously
     */
    @Async
    public CompletableFuture<Message> saveMessage(Long userId, String content) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        Message message = new Message();
        message.setUser(userOpt.get());
        message.setContent(content);
        
        Message saved = messageRepository.save(message);
        log.info("Message saved from user {}: {}", userId, saved.getId());
        
        return CompletableFuture.completedFuture(saved);
    }
    
    /**
     * Get latest messages (paginated)
     */
    public List<Message> getLatestMessages(int limit) {
        if (limit > 1000) limit = 1000; // safety limit
        return messageRepository.findLatestMessages(limit);
    }
    
    /**
     * Get user's messages
     */
    public List<Message> getUserMessages(Long userId) {
        return messageRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get user's message count
     */
    public long getUserMessageCount(Long userId) {
        return messageRepository.countByUserId(userId);
    }
    
    /**
     * Mark message as delivered
     */
    public Message markAsDelivered(Long messageId) {
        Optional<Message> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isPresent()) {
            Message message = msgOpt.get();
            message.setStatus(Message.MessageStatus.DELIVERED);
            message.setUpdatedAt(LocalDateTime.now());
            return messageRepository.save(message);
        }
        throw new IllegalArgumentException("Message not found: " + messageId);
    }
    
    /**
     * Mark message as read
     */
    public Message markAsRead(Long messageId) {
        Optional<Message> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isPresent()) {
            Message message = msgOpt.get();
            message.setStatus(Message.MessageStatus.READ);
            message.setUpdatedAt(LocalDateTime.now());
            return messageRepository.save(message);
        }
        throw new IllegalArgumentException("Message not found: " + messageId);
    }
    
    /**
     * Delete message
     */
    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
        log.info("Message deleted: {}", messageId);
    }
    
    /**
     * Bulk delete messages by user
     */
    @Async
    public CompletableFuture<Void> deleteUserMessages(Long userId) {
        List<Message> messages = getUserMessages(userId);
        messageRepository.deleteAll(messages);
        log.info("Deleted {} messages for user {}", messages.size(), userId);
        return CompletableFuture.completedFuture(null);
    }
}
