package in.raahi.backend.repository;

import in.raahi.backend.entity.IntercityTrip;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntercityTripRepository extends JpaRepository<IntercityTrip, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM IntercityTrip t WHERE t.id = :id")
    Optional<IntercityTrip> findByIdForUpdate(@Param("id") UUID id);

    List<IntercityTrip> findByAgentIdOrderByCreatedAtDesc(UUID agentId);

    List<IntercityTrip> findByDriverIdOrderByCreatedAtDesc(UUID driverId);

    @Query("SELECT t FROM IntercityTrip t WHERE t.status = 'PUBLISHED' " +
           "AND (cast(:pickupCity as string) IS NULL OR LOWER(t.pickupCity) LIKE LOWER(CONCAT('%', cast(:pickupCity as string), '%'))) " +
           "AND (cast(:destCity as string) IS NULL OR LOWER(t.destCity) LIKE LOWER(CONCAT('%', cast(:destCity as string), '%'))) " +
           "AND (cast(:vehicleCategory as string) IS NULL OR LOWER(t.vehicleCategory) = LOWER(cast(:vehicleCategory as string))) " +
           "AND (cast(:fromTime as java.time.Instant) IS NULL OR t.departureTime >= :fromTime) " +
           "ORDER BY t.departureTime ASC")
    List<IntercityTrip> searchTrips(
            @Param("pickupCity") String pickupCity,
            @Param("destCity") String destCity,
            @Param("vehicleCategory") String vehicleCategory,
            @Param("fromTime") Instant fromTime
    );

    @Query("SELECT t FROM IntercityTrip t WHERE t.driver.id = :driverId AND t.status IN ('ASSIGNED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'AWAITING_AGENT_SETTLEMENT', 'SETTLEMENT_SUBMITTED', 'SETTLEMENT_CONFIRMED', 'DISPUTED') ORDER BY t.departureTime ASC")
    List<IntercityTrip> findActiveTripsForDriver(@Param("driverId") UUID driverId);

    @Query("SELECT COUNT(t) FROM IntercityTrip t WHERE t.driver.id = :driverId AND t.status IN ('ASSIGNED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'AWAITING_AGENT_SETTLEMENT', 'SETTLEMENT_SUBMITTED', 'SETTLEMENT_CONFIRMED', 'DISPUTED')")
    long countActiveTripsForDriver(@Param("driverId") UUID driverId);

    @Query("SELECT t FROM IntercityTrip t WHERE t.agent.id = :agentId AND t.status IN ('ASSIGNED', 'EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'AWAITING_AGENT_SETTLEMENT', 'SETTLEMENT_SUBMITTED', 'SETTLEMENT_CONFIRMED', 'DISPUTED') ORDER BY t.departureTime ASC")
    List<IntercityTrip> findActiveTripsForAgent(@Param("agentId") UUID agentId);
}
