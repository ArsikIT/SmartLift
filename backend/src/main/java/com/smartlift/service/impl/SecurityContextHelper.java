package com.smartlift.service.impl;

import com.smartlift.exception.BadRequestException;
import com.smartlift.exception.ResourceNotFoundException;
import com.smartlift.model.Lift;
import com.smartlift.model.User;
import com.smartlift.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextHelper {

    private final UserRepository userRepository;

    public User resolveUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (user.getOrganization() == null) {
            throw new BadRequestException("User has no organization");
        }

        return user;
    }

    public void checkLiftBelongsToOrg(Lift lift, Long organizationId) {
        boolean belongs = isOrgMatch(lift.getManufacturerOrganization(), organizationId)
                || isOrgMatch(lift.getServiceOrganization(), organizationId)
                || isOrgMatch(lift.getManagementOrganization(), organizationId);

        if (!belongs) {
            throw new AccessDeniedException("Access denied: lift does not belong to your organization");
        }
    }

    private boolean isOrgMatch(com.smartlift.model.Organization org, Long organizationId) {
        return org != null && org.getId().equals(organizationId);
    }
}
