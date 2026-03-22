package com.smartlift.repository;

import com.smartlift.model.Lift;
import com.smartlift.model.enums.LiftStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LiftRepository extends JpaRepository<Lift, Long> {

    Optional<Lift> findBySerialNumber(String serialNumber);

    boolean existsBySerialNumber(String serialNumber);

    Page<Lift> findAllByStatus(LiftStatus status, Pageable pageable);

    Page<Lift> findAllByManufacturerOrganizationId(Long organizationId, Pageable pageable);

    Page<Lift> findAllByServiceOrganizationId(Long organizationId, Pageable pageable);

    Page<Lift> findAllByManagementOrganizationId(Long organizationId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "manufacturerOrganization",
            "serviceOrganization",
            "managementOrganization"
    })
    Page<Lift> findAll(Pageable pageable);

    @Query("""
            select l
            from Lift l
            left join fetch l.manufacturerOrganization
            left join fetch l.serviceOrganization
            left join fetch l.managementOrganization
            where l.id = :id
            """)
    Optional<Lift> findDetailedById(Long id);
}
