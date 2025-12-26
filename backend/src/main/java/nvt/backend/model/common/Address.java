package nvt.backend.model.common;

import jakarta.persistence.*;
import lombok.Data;

@Embeddable
@Data
public class Address {

    private String street;
    private String city;
    private String number;
    private double longitude;
    private double latitude;
}
