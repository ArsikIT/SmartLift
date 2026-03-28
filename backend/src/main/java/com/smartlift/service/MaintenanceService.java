package com.smartlift.service;

import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.response.MaintenanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MaintenanceService {

    Page<MaintenanceResponse> getAllMaintenances(String currentUsername, Pageable pageable);

    Page<MaintenanceResponse> getMaintenancesByLiftId(String currentUsername, Long liftId, Pageable pageable);

    MaintenanceResponse getMaintenanceById(String currentUsername, Long id);

    MaintenanceResponse createMaintenance(String currentUsername, MaintenanceRequest request);

    MaintenanceResponse updateMaintenance(String currentUsername, Long id, MaintenanceRequest request);

    void deleteMaintenance(String currentUsername, Long id);
}
