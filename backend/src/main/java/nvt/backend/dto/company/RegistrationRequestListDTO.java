package nvt.backend.dto.company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.company.RegistrationRequest;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestListDTO implements Serializable {
    private Long id;
    private String companyName;
    private String countryName;
    private String cityName;
    private String street;
    private String streetNumber;
    private double latitude;
    private double longitude;
    private String status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String ownerName;
    private String ownerEmail;

    public static RegistrationRequestListDTO fromEntity(RegistrationRequest request) {
        return RegistrationRequestListDTO.builder()
                .id(request.getId())
                .companyName(request.getName())
                .countryName(request.getCountry().getName())
                .cityName(request.getCity().getName())
                .street(request.getStreet())
                .streetNumber(request.getStreetNumber())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(request.getStatus().name())
                .rejectionReason(request.getRejectionReason())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .ownerName(request.getOwner().getName() + " " + request.getOwner().getSurname())
                .ownerEmail(request.getOwner().getUsername())
                .build();
    }
}
