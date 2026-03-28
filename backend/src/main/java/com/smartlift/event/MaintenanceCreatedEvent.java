package com.smartlift.event;

import com.smartlift.model.Maintenance;
import org.springframework.context.ApplicationEvent;

public class MaintenanceCreatedEvent extends ApplicationEvent {

    private final Maintenance maintenance;

    public MaintenanceCreatedEvent(Object source, Maintenance maintenance) {
        super(source);
        this.maintenance = maintenance;
    }

    public Maintenance getMaintenance() {
        return maintenance;
    }
}
