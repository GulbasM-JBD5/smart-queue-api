package com.example.smartqueueapi.repository;

import com.example.smartqueueapi.entity.QueueCustomer;
import com.example.smartqueueapi.entity.QueueStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.List;
import java.util.Optional;
public interface QueueCustomerRepository extends JpaRepository<QueueCustomer, Long> {
    List<QueueCustomer> findByStatusOrderByCreatedAtAscIdAsc(QueueStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QueueCustomer> findFirstByStatusOrderByCreatedAtAscIdAsc(QueueStatus status);
}
