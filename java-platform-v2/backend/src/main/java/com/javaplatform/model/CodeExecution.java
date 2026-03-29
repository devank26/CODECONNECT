package com.javaplatform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_executions", indexes = {
    @Index(name = "idx_execution_user", columnList = "user_id, created_at DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(columnDefinition = "LONGTEXT")
    private String code;
    
    @Column(columnDefinition = "LONGTEXT")
    private String output;
    
    @Column(columnDefinition = "LONGTEXT")
    private String errors;
    
    @Column(name = "execution_time")
    private Long executionTime;
    
    @Column(name = "is_success")
    private boolean success;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
