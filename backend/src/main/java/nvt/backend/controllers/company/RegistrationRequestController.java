package nvt.backend.controllers.company;


import lombok.RequiredArgsConstructor;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.company.CreateRequestDTO;
import nvt.backend.dto.company.ProcessRequestDTO;
import nvt.backend.dto.company.RegistrationRequestListDTO;
import nvt.backend.dto.company.RegistrationRequestResponseDTO;
import nvt.backend.model.company.RegistrationRequest;
import nvt.backend.model.user.User;
import nvt.backend.repositories.user.UserRepository;
import nvt.backend.services.company.RegistrationRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/v1/registration-requests")
@RequiredArgsConstructor
public class RegistrationRequestController {

    private final RegistrationRequestService service;
    private final UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("data") CreateRequestDTO dto,
            @RequestPart("images") List<MultipartFile> images,
            @RequestPart("documents") List<MultipartFile> documents,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            User owner = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
            RegistrationRequest request = service.create(dto, images, documents, owner);
            return ResponseEntity.status(HttpStatus.CREATED).body(request.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<List<RegistrationRequestListDTO>> getPendingRequests() {
        return ResponseEntity.ok(service.getPendingRequests());
    }

    @GetMapping("/pending/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<PageResponseDTO<RegistrationRequestListDTO>> getPendingRequestsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(service.getPendingRequestsPaged(page, size, sortBy, sortDir));
    }

    @GetMapping("/pending/count")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<Long> getPendingRequestsCount() {
        return ResponseEntity.ok(service.getPendingRequestsCount());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<List<RegistrationRequestListDTO>> getAllRequests() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    @GetMapping("/all/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<PageResponseDTO<RegistrationRequestListDTO>> getAllRequestsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(service.getAllRequestsPaged(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<?> getRequestById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getRequestById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/process")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<?> processRequest(
            @PathVariable Long id,
            @RequestBody ProcessRequestDTO dto) {
        try {
            RegistrationRequestResponseDTO response = service.processRequest(id, dto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}