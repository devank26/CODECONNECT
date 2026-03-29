package com.javaplatform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(unique = true, length = 100)
    private String sessionId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_active")
    private LocalDateTime lastActive;
    
    @Column(name = "is_online")
    private boolean online;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActive = LocalDateTime.now();
        online = true;
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastActive = LocalDateTime.now();
    }
}
