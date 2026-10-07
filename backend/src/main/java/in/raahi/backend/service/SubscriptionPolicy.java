package in.raahi.backend.service;

import in.raahi.backend.entity.Subscription;
import in.raahi.backend.entity.User;
import in.raahi.backend.exception.ApiException;
import in.raahi.backend.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Single place that decides whether a DRIVER's subscription gates a feature.
 *
 * MVP: payment/subscription lifecycle (checkout, activation, renewal) is not implemented, so
 * enforcement of job creation is OFF by default (raahi.subscription.enforce-job-creation=false).
 * Nothing is faked: no subscription rows are created or marked ACTIVE, and no payment success is
 * simulated. When the real Razorpay lifecycle ships, flip the flag via env var
 * RAAHI_SUBSCRIPTION_ENFORCE_JOB_CREATION=true. The check itself is unchanged and extensible
 * (add more gated features by adding methods here).
 */
@Component
public class SubscriptionPolicy {

    private final SubscriptionRepository subscriptionRepository;
    private final boolean enforceJobCreation;

    public SubscriptionPolicy(SubscriptionRepository subscriptionRepository,
                              @Value("${raahi.subscription.enforce-job-creation:false}") boolean enforceJobCreation) {
        this.subscriptionRepository = subscriptionRepository;
        this.enforceJobCreation = enforceJobCreation;
    }

    public void requireForJobCreation(User requester) {
        if (!enforceJobCreation || requester.getRole() != User.Role.DRIVER) {
            return;
        }
        Subscription sub = subscriptionRepository.findByUserId(requester.getId()).orElse(null);
        if (sub == null || !sub.isCurrentlyActive()) {
            throw ApiException.paymentRequired("SUBSCRIPTION_EXPIRED",
                    "Your subscription has expired. Please renew to continue.");
        }
    }
}
