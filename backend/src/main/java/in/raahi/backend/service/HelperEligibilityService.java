package in.raahi.backend.service;

import in.raahi.backend.entity.MechanicProfile;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.MechanicProfileRepository;
import in.raahi.backend.repository.UserRepository;
import in.raahi.backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

/**
 * Server-side gate for everything a helper can do. Role is ALWAYS read from the database,
 * never from the JWT claim or from which endpoint was called.
 */
@Service
public class HelperEligibilityService {

    private final UserRepository userRepository;
    private final MechanicProfileRepository profileRepository;
    private final CommissionService commissionService;
    private final CancellationPolicyService cancellationPolicy;

    public HelperEligibilityService(UserRepository userRepository, MechanicProfileRepository profileRepository,
                                    CommissionService commissionService, CancellationPolicyService cancellationPolicy) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.commissionService = commissionService;
        this.cancellationPolicy = cancellationPolicy;
    }

    /** APPROVED + ACTIVE + not temporarily blocked. Used for going online and for viewing open requests. */
    public User requireApprovedActive(AuthenticatedUser principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
        if (user.getRole() != User.Role.HELPER && user.getRole() != User.Role.MECHANIC) {
            throw ApiException.forbidden("FORBIDDEN", "Only approved helpers or mechanics can do this");
        }
        if (user.getStatus() != User.Status.ACTIVE) {
            throw ApiException.forbidden("ACCOUNT_NOT_ACTIVE", "Your account is not active");
        }
        cancellationPolicy.requireNotBlocked(user);
        MechanicProfile profile = profileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null || profile.getVerificationStatus() != MechanicProfile.VerificationStatus.APPROVED) {
            if (profile != null && profile.getVerificationStatus() == MechanicProfile.VerificationStatus.SUSPENDED) {
                throw ApiException.forbidden("HELPER_SUSPENDED", "Your helper account is suspended. Contact support.");
            }
            throw ApiException.forbidden("HELPER_NOT_APPROVED", "Your helper account is not approved yet");
        }
        return user;
    }

    /** Everything above PLUS no outstanding commission. Used for accepting a job. */
    public User requireCanAccept(AuthenticatedUser principal) {
        User user = requireApprovedActive(principal);
        commissionService.requireNoDue(user.getId());
        return user;
    }
}
