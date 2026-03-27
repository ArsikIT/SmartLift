package com.smartlift.model.enums;

import java.util.Set;

public enum LiftStatus {

    CREATED,
    INSTALLED,
    ACTIVE,
    FAULTY,
    IN_REPAIR,
    DECOMMISSIONED;

    public Set<LiftStatus> allowedTransitions() {
        return switch (this) {
            case CREATED -> Set.of(INSTALLED, DECOMMISSIONED);
            case INSTALLED -> Set.of(ACTIVE, DECOMMISSIONED);
            case ACTIVE -> Set.of(FAULTY, DECOMMISSIONED);
            case FAULTY -> Set.of(IN_REPAIR, DECOMMISSIONED);
            case IN_REPAIR -> Set.of(ACTIVE, DECOMMISSIONED);
            case DECOMMISSIONED -> Set.of();
        };
    }

    public boolean canTransitionTo(LiftStatus target) {
        return allowedTransitions().contains(target);
    }
}
