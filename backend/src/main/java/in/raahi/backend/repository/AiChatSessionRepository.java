package in.raahi.backend.repository;

import in.raahi.backend.entity.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, UUID> {
    List<AiChatSession> findByUserIdOrderByLastMessageAtDesc(UUID userId);
    long countByUserId(UUID userId);

    @Query("SELECT s FROM AiChatSession s JOIN FETCH s.user WHERE s.id = :id")
    Optional<AiChatSession> findByIdWithUser(@Param("id") UUID id);
}
