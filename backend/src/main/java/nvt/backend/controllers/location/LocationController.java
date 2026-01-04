package nvt.backend.controllers.location;


import lombok.RequiredArgsConstructor;
import nvt.backend.model.common.City;
import nvt.backend.model.common.Country;
import nvt.backend.repositories.common.CityRepository;
import nvt.backend.repositories.common.CountryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    @GetMapping("/countries")
    public List<Country> getCountries() {
        return countryRepository.findAll();
    }

    @GetMapping("/countries/{id}/cities")
    public List<City> getCities(@PathVariable Long id) {
        return cityRepository.findByCountryId(id);
    }
}