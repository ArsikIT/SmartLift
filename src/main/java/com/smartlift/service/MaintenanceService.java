package com.smartlift.service;

import com.smartlift.dto.request.MaintenanceRequest;
import com.smartlift.dto.response.MaintenanceResponse;
import java.util.List;

public interface MaintenanceService {

    List<MaintenanceResponse> getAllMaintenances();

    List<MaintenanceResponse> getMaintenancesByLiftId(Long liftId);

    MaintenanceResponse getMaintenanceById(Long id);

    MaintenanceResponse createMaintenance(MaintenanceRequest request);

    MaintenanceResponse updateMaintenance(Long id, MaintenanceRequest request);

    void deleteMaintenance(Long id);
}
