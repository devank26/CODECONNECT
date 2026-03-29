package com.javaplatform.api;

import com.javaplatform.model.User;
import com.javaplatform.service.UserService;
import com.javaplatform.util.JwtTokenProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin("*")
public class AuthenticationController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    /**
     * Register new user
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        
        try {
            User user = userService.registerUser(username);
            String token = jwtTokenProvider.generateToken(
                user.getId().toString(), 
                user.getUsername()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("token", token);
            response.put("sessionId", user.getSessionId());
            
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return error;
        }
    }
    
    /**
     * Login user
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        
        try {
            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found");
            }
            
            User user = userOpt.get();
            user = userService.markUserOnline(user.getSessionId());
            
            String token = jwtTokenProvider.generateToken(
                user.getId().toString(), 
                user.getUsername()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("token", token);
            response.put("sessionId", user.getSessionId());
            
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return error;
        }
    }
    
    /**
     * Logout user
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            
            Optional<User> userOpt = userService.getUserBySessionId(userId);
            if (userOpt.isPresent()) {
                userService.markUserOffline(userOpt.get().getSessionId());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout successful");
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return error;
        }
    }
    
    /**
     * Validate token
     */
    @GetMapping("/validate")
    public Map<String, Object> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            boolean valid = jwtTokenProvider.validateToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", valid);
            if (valid) {
                response.put("userId", jwtTokenProvider.getUserIdFromToken(token));
                response.put("username", jwtTokenProvider.getUsernameFromToken(token));
            }
            return response;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("valid", false);
            error.put("message", e.getMessage());
            return error;
        }
    }
}
