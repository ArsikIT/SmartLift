package com.smartlift.repository;

import com.smartlift.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findAllByLiftId(Long liftId, Pageable pageable);

    Page<Document> findAllByMaintenanceId(Long maintenanceId, Pageable pageable);
}
