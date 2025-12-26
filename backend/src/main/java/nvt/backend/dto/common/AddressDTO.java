package nvt.backend.dto.common;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AddressDTO {
    private String street;
    private String city;
    private String number;
    private double longitude;
    private double latitude;
}
