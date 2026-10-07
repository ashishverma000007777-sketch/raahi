package in.raahi.backend.controller;

import in.raahi.backend.dto.ApiResponse;
import in.raahi.backend.dto.DailyDtos.StreakDto;
import in.raahi.backend.entity.DailyCheckin;
import in.raahi.backend.entity.DailyStreak;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.places.PlacesProvider;
import in.raahi.backend.repository.*;
import in.raahi.backend.security.AuthenticatedUser;
import in.raahi.backend.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DailyControllerTest {

    private DailyStreakRepository streakRepository;
    private DailyCheckinRepository checkinRepository;
    private HighwayAlertRepository alertRepository;
    private HighwayAlertVoteRepository voteRepository;
    private DailyTipRepository tipRepository;
    private UserRepository userRepository;
    private PlacesProvider placesProvider;
    private RateLimiter rateLimiter;
    private DailyController controller;

    private AuthenticatedUser principal;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        streakRepository = mock(DailyStreakRepository.class);
        checkinRepository = mock(DailyCheckinRepository.class);
        alertRepository = mock(HighwayAlertRepository.class);
        voteRepository = mock(HighwayAlertVoteRepository.class);
        tipRepository = mock(DailyTipRepository.class);
        userRepository = mock(UserRepository.class);
        placesProvider = mock(PlacesProvider.class);
        rateLimiter = mock(RateLimiter.class);

        controller = new DailyController(
                streakRepository,
                checkinRepository,
                alertRepository,
                voteRepository,
                tipRepository,
                userRepository,
                placesProvider,
                rateLimiter
        );

        userId = UUID.randomUUID();
        principal = new AuthenticatedUser(userId, "DRIVER");

        testUser = new User();
        testUser.setId(userId);
        testUser.setName("Test Driver");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(testUser));
        when(rateLimiter.allow(anyString(), anyInt(), any())).thenReturn(true);
    }

    @Test
    void testCheckinBlockedByRateLimiter() {
        when(rateLimiter.allow(anyString(), anyInt(), any())).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> controller.checkin(principal));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.status);
        assertEquals("RATE_LIMIT_EXCEEDED", ex.code);
    }

    @Test
    void testCheckinThrowsConflictWhenAlreadyCheckedIn() {
        when(checkinRepository.existsByUserIdAndCheckinDate(eq(userId), any(LocalDate.class))).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> controller.checkin(principal));
        assertEquals(HttpStatus.CONFLICT, ex.status);
        assertEquals("ALREADY_CHECKED_IN", ex.code);
        verify(checkinRepository, never()).saveAndFlush(any());
    }

    @Test
    void testCheckinThrowsConflictOnConcurrentRaceDataIntegrityViolation() {
        when(checkinRepository.existsByUserIdAndCheckinDate(eq(userId), any(LocalDate.class))).thenReturn(false);
        when(checkinRepository.saveAndFlush(any(DailyCheckin.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        ApiException ex = assertThrows(ApiException.class, () -> controller.checkin(principal));
        assertEquals(HttpStatus.CONFLICT, ex.status);
        assertEquals("ALREADY_CHECKED_IN", ex.code);
    }

    @Test
    void testCheckinSuccessfulFirstTimeCreatesStreak() {
        when(checkinRepository.existsByUserIdAndCheckinDate(eq(userId), any(LocalDate.class))).thenReturn(false);
        when(streakRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.empty());
        when(streakRepository.save(any(DailyStreak.class))).thenAnswer(i -> i.getArgument(0));

        ApiResponse<StreakDto> response = controller.checkin(principal);

        assertNotNull(response.data);
        assertEquals(1, response.data.currentStreak);
        verify(checkinRepository, times(1)).saveAndFlush(any(DailyCheckin.class));
        verify(streakRepository, times(1)).save(any(DailyStreak.class));
    }
}
