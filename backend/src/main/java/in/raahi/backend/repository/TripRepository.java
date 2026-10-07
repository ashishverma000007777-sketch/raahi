package in.raahi.backend.repository;

import in.raahi.backend.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    @Query("SELECT t FROM Trip t LEFT JOIN FETCH t.vehicle WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<Trip> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT t FROM Trip t LEFT JOIN FETCH t.stops LEFT JOIN FETCH t.vehicle WHERE t.id = :id AND t.user.id = :userId")
    Optional<Trip> findByIdAndUserIdWithStops(@Param("id") UUID id, @Param("userId") UUID userId);

    Optional<Trip> findByIdAndUserId(UUID id, UUID userId);

    Optional<Trip> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, Trip.Status status);
}
