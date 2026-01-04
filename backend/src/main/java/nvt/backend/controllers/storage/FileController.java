package nvt.backend.controllers.storage;

import lombok.RequiredArgsConstructor;
import nvt.backend.model.company.CompanyDocument;
import nvt.backend.model.company.CompanyImage;
import nvt.backend.model.vehicle.VehicleImage;
import nvt.backend.repositories.company.RegistrationRequestRepository;
import nvt.backend.repositories.vehicle.VehicleRepository;
import nvt.backend.services.storage.MinioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioService minioService;
    private final RegistrationRequestRepository registrationRequestRepository;
    private final VehicleRepository vehicleRepository;

    @GetMapping("/image/{imageId}/url")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER', 'MANAGER')")
    public ResponseEntity<Map<String, String>> getImageUrl(@PathVariable Long imageId) {
        var request = registrationRequestRepository.findByImageId(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        CompanyImage image = request.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image not found"));

        String url = minioService.getPresignedUrl(
                image.getMinioBucket(),
                image.getMinioPath(),
                60
        );

        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("originalName", image.getOriginalName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/document/{documentId}/url")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER', 'MANAGER')")
    public ResponseEntity<Map<String, String>> getDocumentUrl(@PathVariable Long documentId) {
        var request = registrationRequestRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        CompanyDocument document = request.getDocuments().stream()
                .filter(doc -> doc.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Document not found"));

        String url = minioService.getPresignedUrl(
                document.getMinioBucket(),
                document.getMinioPath(),
                60
        );

        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("originalName", document.getOriginalName());
        response.put("contentType", document.getContentType());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/request/{requestId}/files")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CUSTOMER', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getRequestFiles(@PathVariable Long requestId) {
        var request = registrationRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Map<String, Object> response = new HashMap<>();

        var imageUrls = request.getImages().stream()
                .map(img -> Map.of(
                        "id", img.getId(),
                        "originalName", img.getOriginalName(),
                        "url", minioService.getPresignedUrl(img.getMinioBucket(), img.getMinioPath(), 60)
                ))
                .toList();

        var documentUrls = request.getDocuments().stream()
                .map(doc -> Map.of(
                        "id", doc.getId(),
                        "originalName", doc.getOriginalName(),
                        "contentType", doc.getContentType(),
                        "url", minioService.getPresignedUrl(doc.getMinioBucket(), doc.getMinioPath(), 60)
                ))
                .toList();

        response.put("images", imageUrls);
        response.put("documents", documentUrls);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/vehicle-image/{imageId}/url")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, String>> getVehicleImageUrl(@PathVariable Long imageId) {
        var vehicle = vehicleRepository.findByImageId(imageId)
                .orElseThrow(() -> new RuntimeException("Vehicle image not found"));

        VehicleImage image = vehicle.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Image not found"));

        String url = minioService.getPresignedUrl(
                image.getMinioBucket(),
                image.getMinioPath(),
                60
        );

        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        response.put("originalName", image.getOriginalName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/vehicle/{vehicleId}/images")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getVehicleImages(@PathVariable Long vehicleId) {
        var vehicle = vehicleRepository.findByIdWithDetails(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        Map<String, Object> response = new HashMap<>();

        var imageUrls = vehicle.getImages().stream()
                .map(img -> Map.of(
                        "id", img.getId(),
                        "originalName", img.getOriginalName(),
                        "url", minioService.getPresignedUrl(img.getMinioBucket(), img.getMinioPath(), 60)
                ))
                .toList();

        response.put("images", imageUrls);

        return ResponseEntity.ok(response);
    }
}