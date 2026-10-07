package in.raahi.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_location_name", nullable = false)
    private String startLocationName;

    @Column(name = "start_lat", nullable = false)
    private Double startLat;

    @Column(name = "start_lng", nullable = false)
    private Double startLng;

    @Column(name = "dest_location_name", nullable = false)
    private String destLocationName;

    @Column(name = "dest_lat", nullable = false)
    private Double destLat;

    @Column(name = "dest_lng", nullable = false)
    private Double destLng;

    @Column(name = "distance_km", nullable = false)
    private BigDecimal distanceKm;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "fuel_type")
    private String fuelType;

    @Column(name = "tank_full")
    private Boolean tankFull = false;

    @Column(name = "passengers")
    private Integer passengers = 1;

    @Column(name = "preferred_route")
    private String preferredRoute;

    @Column(name = "estimated_fuel_litres")
    private BigDecimal estimatedFuelLitres;

    @Column(name = "estimated_fuel_cost")
    private BigDecimal estimatedFuelCost;

    @Column(name = "estimated_tolls")
    private BigDecimal estimatedTolls;

    @Column(name = "weather_condition")
    private String weatherCondition;

    @Column(name = "weather_warning")
    private String weatherWarning;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PLANNED;

    @Column(name = "start_odometer_km")
    private Integer startOdometerKm;

    @Column(name = "end_odometer_km")
    private Integer endOdometerKm;

    @Column(name = "actual_fuel_cost")
    private BigDecimal actualFuelCost;

    @Column(name = "actual_litres")
    private BigDecimal actualLitres;

    @Column(name = "stops_visited_count")
    private Integer stopsVisitedCount = 0;

    @Column(name = "driving_score")
    private Integer drivingScore;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<TripStop> stops = new ArrayList<>();

    public enum Status {
        PLANNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStartLocationName() { return startLocationName; }
    public void setStartLocationName(String startLocationName) { this.startLocationName = startLocationName; }

    public Double getStartLat() { return startLat; }
    public void setStartLat(Double startLat) { this.startLat = startLat; }

    public Double getStartLng() { return startLng; }
    public void setStartLng(Double startLng) { this.startLng = startLng; }

    public String getDestLocationName() { return destLocationName; }
    public void setDestLocationName(String destLocationName) { this.destLocationName = destLocationName; }

    public Double getDestLat() { return destLat; }
    public void setDestLat(Double destLat) { this.destLat = destLat; }

    public Double getDestLng() { return destLng; }
    public void setDestLng(Double destLng) { this.destLng = destLng; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }

    public Boolean getTankFull() { return tankFull; }
    public void setTankFull(Boolean tankFull) { this.tankFull = tankFull; }

    public Integer getPassengers() { return passengers; }
    public void setPassengers(Integer passengers) { this.passengers = passengers; }

    public String getPreferredRoute() { return preferredRoute; }
    public void setPreferredRoute(String preferredRoute) { this.preferredRoute = preferredRoute; }

    public BigDecimal getEstimatedFuelLitres() { return estimatedFuelLitres; }
    public void setEstimatedFuelLitres(BigDecimal estimatedFuelLitres) { this.estimatedFuelLitres = estimatedFuelLitres; }

    public BigDecimal getEstimatedFuelCost() { return estimatedFuelCost; }
    public void setEstimatedFuelCost(BigDecimal estimatedFuelCost) { this.estimatedFuelCost = estimatedFuelCost; }

    public BigDecimal getEstimatedTolls() { return estimatedTolls; }
    public void setEstimatedTolls(BigDecimal estimatedTolls) { this.estimatedTolls = estimatedTolls; }

    public String getWeatherCondition() { return weatherCondition; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }

    public String getWeatherWarning() { return weatherWarning; }
    public void setWeatherWarning(String weatherWarning) { this.weatherWarning = weatherWarning; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Integer getStartOdometerKm() { return startOdometerKm; }
    public void setStartOdometerKm(Integer startOdometerKm) { this.startOdometerKm = startOdometerKm; }

    public Integer getEndOdometerKm() { return endOdometerKm; }
    public void setEndOdometerKm(Integer endOdometerKm) { this.endOdometerKm = endOdometerKm; }

    public BigDecimal getActualFuelCost() { return actualFuelCost; }
    public void setActualFuelCost(BigDecimal actualFuelCost) { this.actualFuelCost = actualFuelCost; }

    public BigDecimal getActualLitres() { return actualLitres; }
    public void setActualLitres(BigDecimal actualLitres) { this.actualLitres = actualLitres; }

    public Integer getStopsVisitedCount() { return stopsVisitedCount; }
    public void setStopsVisitedCount(Integer stopsVisitedCount) { this.stopsVisitedCount = stopsVisitedCount; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Integer getDrivingScore() { return drivingScore; }
    public void setDrivingScore(Integer drivingScore) { this.drivingScore = drivingScore; }

    public List<TripStop> getStops() { return stops; }
    public void setStops(List<TripStop> stops) { this.stops = stops; }
}
