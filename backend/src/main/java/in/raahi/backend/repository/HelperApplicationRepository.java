package in.raahi.backend.repository;

import in.raahi.backend.entity.HelperApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HelperApplicationRepository extends JpaRepository<HelperApplication, UUID> {
    @Query("select a from HelperApplication a join fetch a.user left join fetch a.aadhaarFront left join fetch a.aadhaarBack left join fetch a.selfie where a.user.id = :userId")
    Optional<HelperApplication> findByUserId(@Param("userId") UUID userId);

    @Query("select a from HelperApplication a join fetch a.user left join fetch a.aadhaarFront left join fetch a.aadhaarBack left join fetch a.selfie where a.id = :id")
    Optional<HelperApplication> findByIdWithDetails(@Param("id") UUID id);

    @Query("select a from HelperApplication a join fetch a.user left join fetch a.aadhaarFront left join fetch a.aadhaarBack left join fetch a.selfie where a.status = :status order by a.submittedAt asc")
    List<HelperApplication> findByStatusOrderBySubmittedAtAsc(@Param("status") HelperApplication.Status status);
}
