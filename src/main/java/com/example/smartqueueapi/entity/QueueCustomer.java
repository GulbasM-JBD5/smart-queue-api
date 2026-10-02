package com.example.smartqueueapi.entity;
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
@Entity
@Table(name = "queue_customers")
public class QueueCustomer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private QueueStatus status;

    public QueueCustomer() {
    }
    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public QueueStatus getStatus() {
        return status;
    }
    public void setStatus(QueueStatus status) {
        this.status = status;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public void setName(String name) {
        this.name = name;
    }


}
