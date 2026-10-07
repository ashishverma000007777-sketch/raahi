package in.raahi.backend.repository;

import in.raahi.backend.entity.DailyCheckin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyCheckinRepository extends JpaRepository<DailyCheckin, UUID> {
    boolean existsByUserIdAndCheckinDate(UUID userId, LocalDate checkinDate);
    Optional<DailyCheckin> findByUserIdAndCheckinDate(UUID userId, LocalDate checkinDate);
}
