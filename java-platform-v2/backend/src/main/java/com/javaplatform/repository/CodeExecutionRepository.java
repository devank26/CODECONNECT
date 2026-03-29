package com.javaplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.javaplatform.model.CodeExecution;
import java.util.List;

@Repository
public interface CodeExecutionRepository extends JpaRepository<CodeExecution, Long> {
    List<CodeExecution> findByUserId(Long userId);
    List<CodeExecution> findByUserIdAndSuccessTrue(Long userId);
    long countByUserId(Long userId);
    long countByUserIdAndSuccessTrue(Long userId);
}
