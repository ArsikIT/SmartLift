package com.smartlift.repository;

import com.smartlift.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findAllByLiftId(Long liftId, Pageable pageable);

    Page<Document> findAllByMaintenanceId(Long maintenanceId, Pageable pageable);

    @Query("""
            select d from Document d
            left join d.lift l
            left join d.maintenance m
            left join m.lift ml
            where l.manufacturerOrganization.id = :orgId
               or l.serviceOrganization.id = :orgId
               or l.managementOrganization.id = :orgId
               or ml.manufacturerOrganization.id = :orgId
               or ml.serviceOrganization.id = :orgId
               or ml.managementOrganization.id = :orgId
            """)
    Page<Document> findAllByOrganizationId(Long orgId, Pageable pageable);
}
