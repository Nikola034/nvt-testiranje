package nvt.backend.repositories.customer;

import nvt.backend.model.customer.CustomerCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerCompanyRepository extends JpaRepository<CustomerCompany, Long> {

    @Query("SELECT cc FROM CustomerCompany cc " +
           "LEFT JOIN FETCH cc.country " +
           "LEFT JOIN FETCH cc.city " +
           "WHERE cc.owner.id = :ownerId AND cc.active = true")
    List<CustomerCompany> findByOwnerId(@Param("ownerId") Integer ownerId);

    @Query("SELECT cc FROM CustomerCompany cc " +
           "LEFT JOIN FETCH cc.country " +
           "LEFT JOIN FETCH cc.city " +
           "LEFT JOIN FETCH cc.owner " +
           "WHERE cc.id = :id")
    Optional<CustomerCompany> findByIdWithDetails(@Param("id") Long id);

    boolean existsByTaxId(String taxId);

    @Query("SELECT CASE WHEN COUNT(cc) > 0 THEN true ELSE false END " +
           "FROM CustomerCompany cc " +
           "WHERE cc.taxId = :taxId AND cc.id != :companyId")
    boolean existsByTaxIdAndIdNot(@Param("taxId") String taxId, @Param("companyId") Long companyId);

    // Proveri da li kupac ima pristup firmi
    @Query("SELECT CASE WHEN COUNT(cc) > 0 THEN true ELSE false END " +
           "FROM CustomerCompany cc " +
           "WHERE cc.id = :companyId AND cc.owner.id = :ownerId AND cc.active = true")
    boolean isOwnedByUser(@Param("companyId") Long companyId, @Param("ownerId") Integer ownerId);
}
