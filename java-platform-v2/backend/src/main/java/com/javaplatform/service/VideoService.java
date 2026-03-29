package com.javaplatform.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class VideoService {
    
    private final Map<String, VideoSession> sessions = new ConcurrentHashMap<>();
    
    /**
     * Create video call session
     */
    public VideoSession createVideoSession(String callId, String initiator, String recipient) {
        VideoSession session = new VideoSession();
        session.setCallId(callId);
        session.setInitiator(initiator);
        session.setRecipient(recipient);
        session.setStatus("INITIATED");
        session.setCreatedAt(System.currentTimeMillis());
        
        sessions.put(callId, session);
        log.info("Video session created: {} between {} and {}", 
                 callId, initiator, recipient);
        
        return session;
    }
    
    /**
     * Get video session
     */
    public VideoSession getSession(String callId) {
        return sessions.get(callId);
    }
    
    /**
     * Update session status
     */
    public VideoSession updateSessionStatus(String callId, String status) {
        VideoSession session = sessions.get(callId);
        if (session != null) {
            session.setStatus(status);
            session.setLastUpdated(System.currentTimeMillis());
            
            if ("ENDED".equals(status)) {
                session.setEndedAt(System.currentTimeMillis());
            }
        }
        return session;
    }
    
    /**
     * Add WebRTC offer
     */
    public void addOffer(String callId, String sdpOffer) {
        VideoSession session = sessions.get(callId);
        if (session != null) {
            session.setOfferSDP(sdpOffer);
        }
    }
    
    /**
     * Add WebRTC answer
     */
    public void addAnswer(String callId, String sdpAnswer) {
        VideoSession session = sessions.get(callId);
        if (session != null) {
            session.setAnswerSDP(sdpAnswer);
        }
    }
    
    /**
     * Add ICE candidate
     */
    public void addICECandidate(String callId, String candidate) {
        VideoSession session = sessions.get(callId);
        if (session != null) {
            if (session.getIceCandidates() == null) {
                session.setIceCandidates(new ArrayList<>());
            }
            session.getIceCandidates().add(candidate);
        }
    }
    
    /**
     * End video session
     */
    public void endSession(String callId) {
        VideoSession session = sessions.remove(callId);
        if (session != null) {
            long duration = (System.currentTimeMillis() - session.getCreatedAt()) / 1000;
            log.info("Video session ended: {} (duration: {}s)", callId, duration);
        }
    }
    
    /**
     * Get active sessions
     */
    public Collection<VideoSession> getActiveSessions() {
        return sessions.values();
    }
    
    // Inner class for video session with WebRTC data
    public static class VideoSession {
        private String callId;
        private String initiator;
        private String recipient;
        private String status;
        private String offerSDP;
        private String answerSDP;
        private List<String> iceCandidates;
        private long createdAt;
        private long lastUpdated;
        private long endedAt;
        
        // Getters and setters
        public String getCallId() { return callId; }
        public void setCallId(String callId) { this.callId = callId; }
        
        public String getInitiator() { return initiator; }
        public void setInitiator(String initiator) { this.initiator = initiator; }
        
        public String getRecipient() { return recipient; }
        public void setRecipient(String recipient) { this.recipient = recipient; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getOfferSDP() { return offerSDP; }
        public void setOfferSDP(String offerSDP) { this.offerSDP = offerSDP; }
        
        public String getAnswerSDP() { return answerSDP; }
        public void setAnswerSDP(String answerSDP) { this.answerSDP = answerSDP; }
        
        public List<String> getIceCandidates() { return iceCandidates; }
        public void setIceCandidates(List<String> iceCandidates) { this.iceCandidates = iceCandidates; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
        
        public long getEndedAt() { return endedAt; }
        public void setEndedAt(long endedAt) { this.endedAt = endedAt; }
    }
}
