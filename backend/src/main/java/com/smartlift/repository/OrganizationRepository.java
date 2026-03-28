package com.smartlift.repository;

import com.smartlift.model.Organization;
import com.smartlift.model.enums.OrganizationType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByName(String name);

    boolean existsByName(String name);

    Page<Organization> findAllByType(OrganizationType type, Pageable pageable);
}
