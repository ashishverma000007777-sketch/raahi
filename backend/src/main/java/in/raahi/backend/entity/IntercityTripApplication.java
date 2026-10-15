package in.raahi.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "intercity_trip_applications",
       uniqueConstraints = @UniqueConstraint(name = "uq_intercity_trip_driver_app", columnNames = {"trip_id", "driver_id"}))
public class IntercityTripApplication {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private IntercityTrip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "driver_notes")
    private String driverNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public enum ApplicationStatus {
        APPLIED,
        ACCEPTED,
        REJECTED,
        WITHDRAWN
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public IntercityTrip getTrip() { return trip; }
    public void setTrip(IntercityTrip trip) { this.trip = trip; }
    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getDriverNotes() { return driverNotes; }
    public void setDriverNotes(String driverNotes) { this.driverNotes = driverNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
