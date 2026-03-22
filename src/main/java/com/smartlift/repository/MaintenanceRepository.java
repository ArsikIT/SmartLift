package com.smartlift.repository;

import com.smartlift.model.Maintenance;
import com.smartlift.model.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    Page<Maintenance> findAllByLiftId(Long liftId, Pageable pageable);

    Page<Maintenance> findAllByAssignedTechnicianId(Long technicianId, Pageable pageable);

    Page<Maintenance> findAllByStatus(MaintenanceStatus status, Pageable pageable);

    @Query("""
            select m from Maintenance m
            join m.lift l
            where l.manufacturerOrganization.id = :orgId
               or l.serviceOrganization.id = :orgId
               or l.managementOrganization.id = :orgId
            """)
    Page<Maintenance> findAllByOrganizationId(Long orgId, Pageable pageable);
}
