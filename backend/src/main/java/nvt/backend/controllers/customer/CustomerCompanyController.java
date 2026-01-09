package nvt.backend.controllers.customer;

import lombok.RequiredArgsConstructor;
import nvt.backend.config.SecurityUser;
import nvt.backend.dto.customer.CreateCustomerCompanyDTO;
import nvt.backend.dto.customer.CustomerCompanyDTO;
import nvt.backend.model.user.User;
import nvt.backend.repositories.user.UserRepository;
import nvt.backend.services.customer.CustomerCompanyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CustomerCompanyController {

    private final CustomerCompanyService companyService;
    private final UserRepository userRepository;

    /**
     * Kreiranje nove firme kupca (adresa dostave)
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> createCompany(
            @RequestBody CreateCustomerCompanyDTO dto,
            @AuthenticationPrincipal SecurityUser securityUser) {
        try {
            User owner = userRepository.findByUsername(securityUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            CustomerCompanyDTO company = companyService.createCompany(dto, owner);
            return ResponseEntity.status(HttpStatus.CREATED).body(company);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Dohvata sve firme kupca
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<CustomerCompanyDTO>> getMyCompanies(
            @AuthenticationPrincipal SecurityUser securityUser) {
        User owner = userRepository.findByUsername(securityUser.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(companyService.getMyCompanies(owner));
    }

    /**
     * Dohvata firmu po ID-u
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> getCompanyById(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        try {
            User user = userRepository.findByUsername(securityUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            return ResponseEntity.ok(companyService.getCompanyById(id, user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Ažurira firmu kupca
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> updateCompany(
            @PathVariable Long id,
            @RequestBody CreateCustomerCompanyDTO dto,
            @AuthenticationPrincipal SecurityUser securityUser) {
        try {
            User user = userRepository.findByUsername(securityUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            CustomerCompanyDTO company = companyService.updateCompany(id, dto, user);
            return ResponseEntity.ok(company);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Briše firmu kupca (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> deleteCompany(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser securityUser) {
        try {
            User user = userRepository.findByUsername(securityUser.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            companyService.deleteCompany(id, user);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
