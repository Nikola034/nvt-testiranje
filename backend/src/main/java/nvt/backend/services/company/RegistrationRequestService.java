package nvt.backend.services.company;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.company.CreateRequestDTO;
import nvt.backend.dto.company.ProcessRequestDTO;
import nvt.backend.dto.company.RegistrationRequestListDTO;
import nvt.backend.dto.company.RegistrationRequestResponseDTO;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.model.company.CompanyDocument;
import nvt.backend.model.company.CompanyImage;
import nvt.backend.model.company.RegistrationRequest;
import nvt.backend.model.user.User;
import nvt.backend.repositories.common.CityRepository;
import nvt.backend.repositories.common.CountryRepository;
import nvt.backend.repositories.company.RegistrationRequestRepository;
import nvt.backend.services.auth.EmailService;
import nvt.backend.services.storage.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationRequestService {

    private final RegistrationRequestRepository requestRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final MinioService minioService;
    private final EmailService emailService;

    @Value("${spring.mail.sender}")
    private String mailSender;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "requestsPage", allEntries = true),
        @CacheEvict(value = "pendingRequestsCount", allEntries = true)
    })
    public RegistrationRequest create(CreateRequestDTO dto,
                                      List<MultipartFile> images,
                                      List<MultipartFile> documents,
                                      User owner) throws IOException {

        Country country = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));

        City city = cityRepository.findById(dto.getCityId())
                .orElseThrow(() -> new RuntimeException("City not found"));

        RegistrationRequest request = new RegistrationRequest();
        request.setName(dto.getName());
        request.setCountry(country);
        request.setCity(city);
        request.setStreet(dto.getStreet());
        request.setStreetNumber(dto.getStreetNumber());
        request.setLatitude(dto.getLatitude());
        request.setLongitude(dto.getLongitude());
        request.setOwner(owner);

        request = requestRepository.save(request);

        // Upload images to MinIO
        String imagesBucket = minioService.getCompanyImagesBucket();
        for (MultipartFile file : images) {
            String minioPath = minioService.uploadFile(file, imagesBucket, "company-" + request.getId());

            CompanyImage img = new CompanyImage();
            img.setOriginalName(file.getOriginalFilename());
            img.setMinioPath(minioPath);
            img.setMinioBucket(imagesBucket);
            img.setRequest(request);
            request.getImages().add(img);
        }

        // Upload documents to MinIO
        String documentsBucket = minioService.getCompanyDocumentsBucket();
        for (MultipartFile file : documents) {
            String minioPath = minioService.uploadFile(file, documentsBucket, "company-" + request.getId());

            CompanyDocument doc = new CompanyDocument();
            doc.setOriginalName(file.getOriginalFilename());
            doc.setMinioPath(minioPath);
            doc.setMinioBucket(documentsBucket);
            doc.setContentType(file.getContentType());
            doc.setRequest(request);
            request.getDocuments().add(doc);
        }

        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<RegistrationRequestListDTO> getPendingRequests() {
        return requestRepository.findByStatusWithDetails(RegistrationRequest.Status.PENDING)
                .stream()
                .map(RegistrationRequestListDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "requestsPage", key = "'pending-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortDir")
    public PageResponseDTO<RegistrationRequestListDTO> getPendingRequestsPaged(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Long> idPage = requestRepository.findIdsByStatus(RegistrationRequest.Status.PENDING, pageable);
        List<RegistrationRequest> requests = idPage.getContent().isEmpty() 
                ? List.of() 
                : requestRepository.findAllByIds(idPage.getContent());
        
        return PageResponseDTO.<RegistrationRequestListDTO>builder()
                .content(requests.stream().map(RegistrationRequestListDTO::fromEntity).toList())
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RegistrationRequestListDTO> getAllRequests() {
        return requestRepository.findAll()
                .stream()
                .map(RegistrationRequestListDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "requestsPage", key = "'all-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortDir")
    public PageResponseDTO<RegistrationRequestListDTO> getAllRequestsPaged(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Long> idPage = requestRepository.findAllIds(pageable);
        List<RegistrationRequest> requests = idPage.getContent().isEmpty() 
                ? List.of() 
                : requestRepository.findAllByIds(idPage.getContent());
        
        return PageResponseDTO.<RegistrationRequestListDTO>builder()
                .content(requests.stream().map(RegistrationRequestListDTO::fromEntity).toList())
                .page(idPage.getNumber())
                .size(idPage.getSize())
                .totalElements(idPage.getTotalElements())
                .totalPages(idPage.getTotalPages())
                .first(idPage.isFirst())
                .last(idPage.isLast())
                .build();
    }

    @Cacheable(value = "pendingRequestsCount")
    public long getPendingRequestsCount() {
        return requestRepository.countByStatus(RegistrationRequest.Status.PENDING);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "requestById", key = "#id")
    public RegistrationRequestResponseDTO getRequestById(Long id) {
        RegistrationRequest request = requestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Registration request not found"));
        return RegistrationRequestResponseDTO.fromEntity(request);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "requestById", key = "#id"),
        @CacheEvict(value = "requestsPage", allEntries = true),
        @CacheEvict(value = "pendingRequestsCount", allEntries = true)
    })
    public RegistrationRequestResponseDTO processRequest(Long id, ProcessRequestDTO dto) {
        RegistrationRequest request = requestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Registration request not found"));

        if (request.getStatus() != RegistrationRequest.Status.PENDING) {
            throw new RuntimeException("Request has already been processed");
        }

        if (dto.isApproved()) {
            request.setStatus(RegistrationRequest.Status.APPROVED);
            request.setRejectionReason(null);
        } else {
            if (dto.getRejectionReason() == null || dto.getRejectionReason().isBlank()) {
                throw new RuntimeException("Rejection reason is required when rejecting a request");
            }
            request.setStatus(RegistrationRequest.Status.REJECTED);
            request.setRejectionReason(dto.getRejectionReason());
        }

        request.setProcessedAt(LocalDateTime.now());
        request = requestRepository.save(request);

        sendDecisionEmail(request);

        return RegistrationRequestResponseDTO.fromEntity(request);
    }

    private void sendDecisionEmail(RegistrationRequest request) {
        String recipientEmail = request.getOwner().getUsername();
        String subject;
        String body;

        if (request.getStatus() == RegistrationRequest.Status.APPROVED) {
            subject = "Company Registration Request Approved";
            body = buildApprovalEmailBody(request);
        } else {
            subject = "Company Registration Request Rejected";
            body = buildRejectionEmailBody(request);
        }

        emailService.sendMail(mailSender, recipientEmail, subject, body);
    }

    private String buildApprovalEmailBody(RegistrationRequest request) {
        return String.format("""
            <html>
            <body>
                <h2>Company Registration Approved</h2>
                <p>Dear %s,</p>
                <p>We are pleased to inform you that your company registration request has been approved.</p>
                <p><strong>Company Name:</strong> %s</p>
                <p><strong>Location:</strong> %s, %s, %s</p>
                <p>You can now access all company features on our platform.</p>
                <br>
                <p>Best regards,</p>
                <p>The Smartly Team</p>
            </body>
            </html>
            """,
                request.getOwner().getName(),
                request.getName(),
                request.getStreet(),
                request.getCity().getName(),
                request.getCountry().getName()
        );
    }

    private String buildRejectionEmailBody(RegistrationRequest request) {
        return String.format("""
            <html>
            <body>
                <h2>Company Registration Rejected</h2>
                <p>Dear %s,</p>
                <p>We regret to inform you that your company registration request has been rejected.</p>
                <p><strong>Company Name:</strong> %s</p>
                <p><strong>Reason:</strong> %s</p>
                <p>If you believe this decision was made in error or have additional information to provide, 
                   please submit a new registration request with the required corrections.</p>
                <br>
                <p>Best regards,</p>
                <p>The Smartly Team</p>
            </body>
            </html>
            """,
                request.getOwner().getName(),
                request.getName(),
                request.getRejectionReason()
        );
    }
}