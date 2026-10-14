package com.bookstore.repository;

import com.bookstore.domain.agent.AgentConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentConversationRepository extends JpaRepository<AgentConversation, UUID> {
    Optional<AgentConversation> findByCorrelationId(String correlationId);
}
