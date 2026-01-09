package nvt.backend.services.customer;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.customer.CreateCustomerCompanyDTO;
import nvt.backend.dto.customer.CustomerCompanyDTO;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.model.customer.CustomerCompany;
import nvt.backend.model.user.User;
import nvt.backend.repositories.common.CityRepository;
import nvt.backend.repositories.common.CountryRepository;
import nvt.backend.repositories.customer.CustomerCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerCompanyService {

    private final CustomerCompanyRepository companyRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    @Transactional
    public CustomerCompanyDTO createCompany(CreateCustomerCompanyDTO dto, User owner) {
        // Provera da li PIB već postoji
        if (dto.getTaxId() != null && companyRepository.existsByTaxId(dto.getTaxId())) {
            throw new RuntimeException("Firma sa ovim PIB-om već postoji");
        }

        Country country = null;
        City city = null;

        if (dto.getCountryId() != null) {
            country = countryRepository.findById(dto.getCountryId())
                    .orElseThrow(() -> new RuntimeException("Država nije pronađena"));
        }

        if (dto.getCityId() != null) {
            city = cityRepository.findById(dto.getCityId())
                    .orElseThrow(() -> new RuntimeException("Grad nije pronađen"));
        }

        CustomerCompany company = new CustomerCompany();
        company.setName(dto.getName());
        company.setTaxId(dto.getTaxId());
        company.setRegistrationNumber(dto.getRegistrationNumber());
        company.setCountry(country);
        company.setCity(city);
        company.setStreet(dto.getStreet());
        company.setStreetNumber(dto.getStreetNumber());
        company.setPostalCode(dto.getPostalCode());
        company.setLatitude(dto.getLatitude());
        company.setLongitude(dto.getLongitude());
        company.setContactPhone(dto.getContactPhone());
        company.setContactEmail(dto.getContactEmail());
        company.setOwner(owner);
        company.setActive(true);
        company.setVerified(false); // Treba verifikacija od menadžera

        company = companyRepository.save(company);

        return CustomerCompanyDTO.fromEntity(company);
    }

    @Transactional(readOnly = true)
    public List<CustomerCompanyDTO> getMyCompanies(User owner) {
        return companyRepository.findByOwnerId(owner.getId()).stream()
                .map(CustomerCompanyDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerCompanyDTO getCompanyById(Long id, User user) {
        CustomerCompany company = companyRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Firma nije pronađena"));

        // Provera pristupa - samo vlasnik ili menadžer može da vidi
        if (company.getOwner().getId() != user.getId() && 
            !user.getAuthorities().contains("MANAGER") && 
            !user.getAuthorities().contains("ADMIN")) {
            throw new RuntimeException("Nemate pristup ovoj firmi");
        }

        return CustomerCompanyDTO.fromEntity(company);
    }

    @Transactional
    public CustomerCompanyDTO updateCompany(Long id, CreateCustomerCompanyDTO dto, User user) {
        CustomerCompany company = companyRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Firma nije pronađena"));

        // Samo vlasnik može da ažurira
        if (company.getOwner().getId() != user.getId()) {
            throw new RuntimeException("Nemate pristup ovoj firmi");
        }

        // Provera PIB-a ako se menja
        if (dto.getTaxId() != null && !dto.getTaxId().equals(company.getTaxId())) {
            if (companyRepository.existsByTaxIdAndIdNot(dto.getTaxId(), id)) {
                throw new RuntimeException("Firma sa ovim PIB-om već postoji");
            }
            company.setTaxId(dto.getTaxId());
        }

        if (dto.getName() != null) company.setName(dto.getName());
        if (dto.getRegistrationNumber() != null) company.setRegistrationNumber(dto.getRegistrationNumber());
        
        if (dto.getCountryId() != null) {
            Country country = countryRepository.findById(dto.getCountryId())
                    .orElseThrow(() -> new RuntimeException("Država nije pronađena"));
            company.setCountry(country);
        }

        if (dto.getCityId() != null) {
            City city = cityRepository.findById(dto.getCityId())
                    .orElseThrow(() -> new RuntimeException("Grad nije pronađen"));
            company.setCity(city);
        }

        if (dto.getStreet() != null) company.setStreet(dto.getStreet());
        if (dto.getStreetNumber() != null) company.setStreetNumber(dto.getStreetNumber());
        if (dto.getPostalCode() != null) company.setPostalCode(dto.getPostalCode());
        if (dto.getLatitude() != null) company.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) company.setLongitude(dto.getLongitude());
        if (dto.getContactPhone() != null) company.setContactPhone(dto.getContactPhone());
        if (dto.getContactEmail() != null) company.setContactEmail(dto.getContactEmail());

        company = companyRepository.save(company);

        return CustomerCompanyDTO.fromEntity(company);
    }

    @Transactional
    public void deleteCompany(Long id, User user) {
        CustomerCompany company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Firma nije pronađena"));

        if (company.getOwner().getId() != user.getId()) {
            throw new RuntimeException("Nemate pristup ovoj firmi");
        }

        // Soft delete
        company.setActive(false);
        companyRepository.save(company);
    }
}
