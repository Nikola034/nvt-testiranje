package nvt.backend.dto.customer;

import lombok.Data;

@Data
public class CreateCustomerCompanyDTO {
    private String name;
    private String taxId;
    private String registrationNumber;
    private Long countryId;
    private Long cityId;
    private String street;
    private String streetNumber;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private String contactPhone;
    private String contactEmail;
}
