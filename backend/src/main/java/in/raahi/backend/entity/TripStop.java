package in.raahi.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_stops")
public class TripStop {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", nullable = false)
    private StopType stopType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "lat", nullable = false)
    private Double lat;

    @Column(name = "lng", nullable = false)
    private Double lng;

    @Column(name = "distance_km")
    private BigDecimal distanceKm;

    @Column(name = "price_per_litre")
    private BigDecimal pricePerLitre;

    @Column(name = "price_diff_per_litre")
    private BigDecimal priceDiffPerLitre;

    @Column(name = "rating")
    private BigDecimal rating;

    @Column(name = "amenities")
    private String amenities;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder = 0;

    @Column(name = "is_visited")
    private Boolean isVisited = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public enum StopType {
        FUEL,
        DHABA,
        PARKING,
        REST,
        MECHANIC,
        EMERGENCY
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }

    public StopType getStopType() { return stopType; }
    public void setStopType(StopType stopType) { this.stopType = stopType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public BigDecimal getPricePerLitre() { return pricePerLitre; }
    public void setPricePerLitre(BigDecimal pricePerLitre) { this.pricePerLitre = pricePerLitre; }

    public BigDecimal getPriceDiffPerLitre() { return priceDiffPerLitre; }
    public void setPriceDiffPerLitre(BigDecimal priceDiffPerLitre) { this.priceDiffPerLitre = priceDiffPerLitre; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    public Integer getStopOrder() { return stopOrder; }
    public void setStopOrder(Integer stopOrder) { this.stopOrder = stopOrder; }

    public Boolean getVisited() { return isVisited; }
    public void setVisited(Boolean visited) { isVisited = visited; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
