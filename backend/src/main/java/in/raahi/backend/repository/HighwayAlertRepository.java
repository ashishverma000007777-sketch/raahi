package in.raahi.backend.repository;

import in.raahi.backend.entity.HighwayAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HighwayAlertRepository extends JpaRepository<HighwayAlert, UUID> {

    @Query("select a from HighwayAlert a join fetch a.user where a.expiresAt > :now and (:type is null or a.type = :type) order by a.createdAt desc")
    List<HighwayAlert> findActive(@Param("type") String type, @Param("now") Instant now);

    @Query("select a from HighwayAlert a join fetch a.user where a.id = :id")
    Optional<HighwayAlert> findByIdWithUser(@Param("id") UUID id);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update HighwayAlert a set a.upvotes = a.upvotes + 1 where a.id = :id")
    int incrementUpvotes(@Param("id") UUID id);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update HighwayAlert a set a.downvotes = a.downvotes + 1 where a.id = :id")
    int incrementDownvotes(@Param("id") UUID id);
}
