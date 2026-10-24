package in.raahi.backend.repository;

import in.raahi.backend.entity.JobCancellationCompensation;
import in.raahi.backend.entity.JobCancellationCompensation.CompensationStatus;
import in.raahi.backend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobCancellationCompensationRepository extends JpaRepository<JobCancellationCompensation, UUID> {

    @Query("SELECT c FROM JobCancellationCompensation c JOIN FETCH c.job JOIN FETCH c.helper JOIN FETCH c.requester WHERE c.job.id = :jobId")
    Optional<JobCancellationCompensation> findByJobId(@Param("jobId") UUID jobId);

    @Query("SELECT c FROM JobCancellationCompensation c JOIN FETCH c.job JOIN FETCH c.helper JOIN FETCH c.requester WHERE c.id = :id")
    Optional<JobCancellationCompensation> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT c FROM JobCancellationCompensation c JOIN FETCH c.job JOIN FETCH c.helper JOIN FETCH c.requester WHERE (:status IS NULL OR c.status = :status) ORDER BY c.createdAt DESC")
    List<JobCancellationCompensation> findAllByStatusOrderByCreatedAtDesc(@Param("status") CompensationStatus status, Pageable pageable);

    @Query("SELECT COUNT(c) FROM JobCancellationCompensation c WHERE c.helper.id = :helperId AND c.createdAt > :since AND c.status IN ('AUTO_APPROVED', 'ADMIN_APPROVED', 'ELIGIBLE_PENDING_REVIEW')")
    long countRecentClaimsByHelper(@Param("helperId") UUID helperId, @Param("since") Instant since);

    @Modifying
    @Query("UPDATE JobCancellationCompensation c SET c.status = :newStatus, c.approvedAmount = :approvedAmount, c.reviewedBy = :admin, c.reviewedAt = :reviewedAt, c.reviewNotes = :notes, c.updatedAt = :now WHERE c.id = :id AND c.status = in.raahi.backend.entity.JobCancellationCompensation.CompensationStatus.ELIGIBLE_PENDING_REVIEW")
    int transitionPendingStatus(
            @Param("id") UUID id,
            @Param("newStatus") CompensationStatus newStatus,
            @Param("approvedAmount") Double approvedAmount,
            @Param("admin") User admin,
            @Param("reviewedAt") Instant reviewedAt,
            @Param("notes") String notes,
            @Param("now") Instant now);
}
