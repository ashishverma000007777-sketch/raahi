package in.raahi.backend.repository;

import in.raahi.backend.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByFirebaseUid(String firebaseUid);
    Optional<User> findByPhone(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.fcmToken = null WHERE u.id = :userId AND u.fcmToken = :token")
    int clearFcmToken(@Param("userId") UUID userId, @Param("token") String token);
}
