package nvt.backend.repositories.common;

import nvt.backend.model.common.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {}