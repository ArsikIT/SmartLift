package com.smartlift.repository;

import com.smartlift.model.Document;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByLiftId(Long liftId);

    List<Document> findAllByMaintenanceId(Long maintenanceId);
}
