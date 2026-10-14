package com.bookstore.repository;

import com.bookstore.domain.agent.AgentToolExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentToolExecutionRepository extends JpaRepository<AgentToolExecution, UUID> {
    List<AgentToolExecution> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
