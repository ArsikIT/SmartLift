package com.smartlift.service;

import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.response.MaintenanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MaintenanceService {

    Page<MaintenanceResponse> getAllMaintenances(Pageable pageable);

    Page<MaintenanceResponse> getMaintenancesByLiftId(Long liftId, Pageable pageable);

    MaintenanceResponse getMaintenanceById(Long id);

    MaintenanceResponse createMaintenance(MaintenanceRequest request);

    MaintenanceResponse updateMaintenance(Long id, MaintenanceRequest request);

    void deleteMaintenance(Long id);
}
