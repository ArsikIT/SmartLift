package com.smartlift.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationTypeTest {

    @Test
    void toRoleName_manufacturerMapsToManufacturer() {
        assertThat(OrganizationType.MANUFACTURER.toRoleName()).isEqualTo(RoleName.MANUFACTURER);
    }

    @Test
    void toRoleName_serviceMapsToService() {
        assertThat(OrganizationType.SERVICE.toRoleName()).isEqualTo(RoleName.SERVICE);
    }

    @Test
    void toRoleName_managementMapsToManagement() {
        assertThat(OrganizationType.MANAGEMENT.toRoleName()).isEqualTo(RoleName.MANAGEMENT);
    }
}
