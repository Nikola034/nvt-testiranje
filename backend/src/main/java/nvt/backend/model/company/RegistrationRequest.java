package nvt.backend.model.company;

import jakarta.persistence.*;
import lombok.Data;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.model.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "registration_requests", indexes = {
    @Index(name = "idx_request_status", columnList = "status"),
    @Index(name = "idx_request_owner", columnList = "owner_id"),
    @Index(name = "idx_request_created_at", columnList = "createdAt"),
    @Index(name = "idx_request_status_created", columnList = "status, createdAt")
})
@Data
public class RegistrationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    private String street;
    private String streetNumber;
    private double latitude;
    private double longitude;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(length = 1000)
    private String rejectionReason;

    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CompanyImage> images = new HashSet<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CompanyDocument> documents = new HashSet<>();

    public enum Status { PENDING, APPROVED, REJECTED }
}