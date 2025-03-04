// PushSubscriptionRepository.java
package com.api.expo.repository;

import com.api.expo.models.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    List<PushSubscription> findByUserId(String userId);
    Optional<PushSubscription> findByEndpoint(String endpoint);
}