package nvt.backend.model.customer;

import jakarta.persistence.*;
import lombok.Data;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.model.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_companies", indexes = {
    @Index(name = "idx_customer_company_owner", columnList = "owner_id"),
    @Index(name = "idx_customer_company_name", columnList = "name"),
    @Index(name = "idx_customer_company_tax_id", columnList = "taxId")
})
@Data
public class CustomerCompany {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String taxId; // PIB

    private String registrationNumber; // Matični broj

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    private String street;
    private String streetNumber;
    private String postalCode;
    private Double latitude;
    private Double longitude;

    private String contactPhone;
    private String contactEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private boolean active = true;
    private boolean verified = false; // Da li je verifikovana od strane menadžera

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
