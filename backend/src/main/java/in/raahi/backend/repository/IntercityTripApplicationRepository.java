package in.raahi.backend.repository;

import in.raahi.backend.entity.IntercityTripApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntercityTripApplicationRepository extends JpaRepository<IntercityTripApplication, UUID> {

    List<IntercityTripApplication> findByTripIdOrderByCreatedAtAsc(UUID tripId);

    List<IntercityTripApplication> findByDriverIdOrderByCreatedAtDesc(UUID driverId);

    Optional<IntercityTripApplication> findByTripIdAndDriverId(UUID tripId, UUID driverId);

    boolean existsByTripIdAndDriverId(UUID tripId, UUID driverId);
}
