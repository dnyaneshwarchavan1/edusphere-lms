package com.edusphere.lms.repository;

import com.edusphere.lms.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("""
            select message from ChatMessage message
            where (message.sender.id = :userId and message.recipient.id = :otherUserId)
               or (message.sender.id = :otherUserId and message.recipient.id = :userId)
            order by message.createdAt asc
            """)
    List<ChatMessage> findConversation(Long userId, Long otherUserId);
}
