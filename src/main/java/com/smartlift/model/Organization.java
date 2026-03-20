package com.smartlift.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationType type;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String contactEmail;

    @Column(length = 50)
    private String contactPhone;

    @OneToMany(mappedBy = "organization")
    private Set<User> users = new HashSet<>();

    @OneToMany(mappedBy = "manufacturerOrganization")
    private Set<Lift> manufacturedLifts = new HashSet<>();

    @OneToMany(mappedBy = "serviceOrganization")
    private Set<Lift> servicedLifts = new HashSet<>();

    @OneToMany(mappedBy = "managementOrganization")
    private Set<Lift> managedLifts = new HashSet<>();
}
