package com.smartlift.model.enums;

public enum OrganizationType {
    MANUFACTURER,
    SERVICE,
    MANAGEMENT;

    public RoleName toRoleName() {
        return switch (this) {
            case MANUFACTURER -> RoleName.MANUFACTURER;
            case SERVICE -> RoleName.SERVICE;
            case MANAGEMENT -> RoleName.MANAGEMENT;
        };
    }
}
