package com.smartlift.repository;

import com.smartlift.model.Organization;
import com.smartlift.model.enums.OrganizationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByName(String name);

    boolean existsByName(String name);

    List<Organization> findAllByType(OrganizationType type);
}
