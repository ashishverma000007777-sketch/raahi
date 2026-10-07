package in.raahi.backend.repository;

import in.raahi.backend.entity.DailyStreak;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DailyStreakRepository extends JpaRepository<DailyStreak, UUID> {
    Optional<DailyStreak> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DailyStreak s WHERE s.userId = :userId")
    Optional<DailyStreak> findByUserIdForUpdate(@Param("userId") UUID userId);
}
