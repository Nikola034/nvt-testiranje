package nvt.backend.dto.company;

import lombok.Data;

@Data
public class CreateRequestDTO {
    private String name;
    private Long countryId;
    private Long cityId;
    private String street;
    private String streetNumber;
    private Double latitude;
    private Double longitude;
}
