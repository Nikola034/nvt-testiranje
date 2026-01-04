package nvt.backend.dto.company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nvt.backend.model.company.RegistrationRequest;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestResponseDTO implements Serializable {
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
    private List<FileDTO> images;
    private List<FileDTO> documents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileDTO implements Serializable {
        private Long id;
        private String originalName;
        private String contentType;
    }

    public static RegistrationRequestResponseDTO fromEntity(RegistrationRequest request) {
        return RegistrationRequestResponseDTO.builder()
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
                .images(request.getImages().stream()
                        .map(img -> FileDTO.builder()
                                .id(img.getId())
                                .originalName(img.getOriginalName())
                                .build())
                        .toList())
                .documents(request.getDocuments().stream()
                        .map(doc -> FileDTO.builder()
                                .id(doc.getId())
                                .originalName(doc.getOriginalName())
                                .contentType(doc.getContentType())
                                .build())
                        .toList())
                .build();
    }
}
