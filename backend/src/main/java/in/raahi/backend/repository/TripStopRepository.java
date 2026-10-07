package in.raahi.backend.repository;

import in.raahi.backend.entity.TripStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripStopRepository extends JpaRepository<TripStop, UUID> {
    List<TripStop> findByTripIdOrderByStopOrderAsc(UUID tripId);
}
