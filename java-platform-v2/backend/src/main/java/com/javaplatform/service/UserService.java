package com.javaplatform.service;

import com.javaplatform.model.User;
import com.javaplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // In-memory session cache (Redis would be better in production)
    private final Map<String, User> sessionCache = new ConcurrentHashMap<>();
    
    /**
     * Register new user
     */
    public User registerUser(String username) {
        // Check if user exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        
        User user = new User();
        user.setUsername(username);
        user.setOnline(true);
        user.setSessionId(UUID.randomUUID().toString());
        
        User saved = userRepository.save(user);
        sessionCache.put(saved.getSessionId(), saved);
        
        log.info("User registered: {} with sessionId: {}", username, saved.getSessionId());
        return saved;
    }
    
    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * Get user by session ID (cached)
     */
    @Cacheable(value = "users", key = "#sessionId")
    public Optional<User> getUserBySessionId(String sessionId) {
        User cached = sessionCache.get(sessionId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return userRepository.findBySessionId(sessionId);
    }
    
    /**
     * Mark user online
     */
    public User markUserOnline(String sessionId) {
        Optional<User> userOpt = getUserBySessionId(sessionId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setOnline(true);
            user.setLastActive(LocalDateTime.now());
            User updated = userRepository.save(user);
            sessionCache.put(sessionId, updated);
            return updated;
        }
        throw new IllegalArgumentException("User not found: " + sessionId);
    }
    
    /**
     * Mark user offline
     */
    @CacheEvict(value = "users", key = "#sessionId")
    public User markUserOffline(String sessionId) {
        Optional<User> userOpt = getUserBySessionId(sessionId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setOnline(false);
            user.setLastActive(LocalDateTime.now());
            User updated = userRepository.save(user);
            sessionCache.remove(sessionId);
            return updated;
        }
        throw new IllegalArgumentException("User not found: " + sessionId);
    }
    
    /**
     * Get all online users
     */
    public List<User> getOnlineUsers() {
        return sessionCache.values().stream()
            .filter(User::isOnline)
            .toList();
    }
    
    /**
     * Update last activity timestamp
     */
    public void updateActivity(String sessionId) {
        Optional<User> userOpt = getUserBySessionId(sessionId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastActive(LocalDateTime.now());
            userRepository.save(user);
        }
    }
    
    /**
     * Delete user and clear session
     */
    @CacheEvict(value = "users", key = "#sessionId")
    public void deleteUser(String sessionId) {
        Optional<User> userOpt = getUserBySessionId(sessionId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userRepository.delete(user);
            sessionCache.remove(sessionId);
        }
    }
}
