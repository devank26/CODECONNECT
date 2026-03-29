package com.javaplatform.api;

import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@CrossOrigin("*")
public class HealthController {
    
    /**
     * Health check endpoint
     */
    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("uptime", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        response.put("message", "Java Platform v2.0 is running");
        return response;
    }
}
