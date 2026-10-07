package in.raahi.backend.repository;

import in.raahi.backend.entity.VerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, UUID> {
    @Query("SELECT d FROM VerificationDocument d JOIN FETCH d.user WHERE d.id = :id")
    Optional<VerificationDocument> findByIdWithUser(@Param("id") UUID id);
}
