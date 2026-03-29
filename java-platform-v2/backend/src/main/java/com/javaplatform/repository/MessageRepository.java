package com.javaplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.javaplatform.model.Message;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query(value = "SELECT m FROM Message m ORDER BY m.createdAt DESC LIMIT :limit")
    List<Message> findLatestMessages(int limit);
    
    List<Message> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    long countByUserId(Long userId);
}
