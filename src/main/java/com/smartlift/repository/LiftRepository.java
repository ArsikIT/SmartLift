package com.smartlift.repository;

import com.smartlift.model.Lift;
import com.smartlift.model.enums.LiftStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LiftRepository extends JpaRepository<Lift, Long> {

    Optional<Lift> findBySerialNumber(String serialNumber);

    boolean existsBySerialNumber(String serialNumber);

    List<Lift> findAllByStatus(LiftStatus status);

    List<Lift> findAllByManufacturerOrganizationId(Long organizationId);

    List<Lift> findAllByServiceOrganizationId(Long organizationId);

    List<Lift> findAllByManagementOrganizationId(Long organizationId);

    @Override
    @EntityGraph(attributePaths = {
            "manufacturerOrganization",
            "serviceOrganization",
            "managementOrganization"
    })
    List<Lift> findAll();

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
