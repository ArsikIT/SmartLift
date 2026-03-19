package com.smartlift.repository;

import com.smartlift.model.Maintenance;
import com.smartlift.model.MaintenanceStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

    List<Maintenance> findAllByLiftId(Long liftId);

    List<Maintenance> findAllByAssignedTechnicianId(Long technicianId);

    List<Maintenance> findAllByStatus(MaintenanceStatus status);
}
