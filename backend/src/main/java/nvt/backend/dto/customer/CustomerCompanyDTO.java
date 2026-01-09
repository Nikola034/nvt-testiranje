package nvt.backend.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.customer.CustomerCompany;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCompanyDTO implements Serializable {
    private Long id;
    private String name;
    private String taxId;
    private String registrationNumber;
    private Long countryId;
    private String countryName;
    private Long cityId;
    private String cityName;
    private String street;
    private String streetNumber;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String contactPhone;
    private String contactEmail;
    private boolean active;
    private boolean verified;

    public static CustomerCompanyDTO fromEntity(CustomerCompany company) {
        return CustomerCompanyDTO.builder()
                .id(company.getId())
                .name(company.getName())
                .taxId(company.getTaxId())
                .registrationNumber(company.getRegistrationNumber())
                .countryId(company.getCountry() != null ? company.getCountry().getId() : null)
                .countryName(company.getCountry() != null ? company.getCountry().getName() : null)
                .cityId(company.getCity() != null ? company.getCity().getId() : null)
                .cityName(company.getCity() != null ? company.getCity().getName() : null)
                .street(company.getStreet())
                .streetNumber(company.getStreetNumber())
                .postalCode(company.getPostalCode())
                .latitude(company.getLatitude())
                .longitude(company.getLongitude())
                .contactPhone(company.getContactPhone())
                .contactEmail(company.getContactEmail())
                .active(company.isActive())
                .verified(company.isVerified())
                .build();
    }
}
