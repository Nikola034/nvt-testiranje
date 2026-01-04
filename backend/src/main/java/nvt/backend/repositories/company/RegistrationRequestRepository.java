package nvt.backend.repositories.company;

import nvt.backend.model.company.RegistrationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {

    @Query("SELECT r FROM RegistrationRequest r JOIN r.images img WHERE img.id = :imageId")
    Optional<RegistrationRequest> findByImageId(@Param("imageId") Long imageId);

    @Query("SELECT r FROM RegistrationRequest r JOIN r.documents doc WHERE doc.id = :documentId")
    Optional<RegistrationRequest> findByDocumentId(@Param("documentId") Long documentId);

    List<RegistrationRequest> findByStatus(RegistrationRequest.Status status);

    @Query("SELECT r FROM RegistrationRequest r " +
           "LEFT JOIN FETCH r.country " +
           "LEFT JOIN FETCH r.city " +
           "LEFT JOIN FETCH r.owner " +
           "LEFT JOIN FETCH r.images " +
           "LEFT JOIN FETCH r.documents " +
           "WHERE r.id = :id")
    Optional<RegistrationRequest> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT r FROM RegistrationRequest r " +
           "LEFT JOIN FETCH r.country " +
           "LEFT JOIN FETCH r.city " +
           "LEFT JOIN FETCH r.owner " +
           "WHERE r.status = :status")
    List<RegistrationRequest> findByStatusWithDetails(@Param("status") RegistrationRequest.Status status);

    @Query("SELECT r.id FROM RegistrationRequest r WHERE r.status = :status")
    Page<Long> findIdsByStatus(@Param("status") RegistrationRequest.Status status, Pageable pageable);

    @Query("SELECT r.id FROM RegistrationRequest r")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT DISTINCT r FROM RegistrationRequest r " +
           "LEFT JOIN FETCH r.country " +
           "LEFT JOIN FETCH r.city " +
           "LEFT JOIN FETCH r.owner " +
           "WHERE r.id IN :ids")
    List<RegistrationRequest> findAllByIds(@Param("ids") List<Long> ids);

    @Query("SELECT DISTINCT r FROM RegistrationRequest r " +
           "LEFT JOIN FETCH r.country " +
           "LEFT JOIN FETCH r.city " +
           "LEFT JOIN FETCH r.owner " +
           "LEFT JOIN FETCH r.images " +
           "LEFT JOIN FETCH r.documents " +
           "WHERE r.id IN :ids")
    List<RegistrationRequest> findAllWithDetailsByIds(@Param("ids") List<Long> ids);

    long countByStatus(RegistrationRequest.Status status);
}