package com.smartlift.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "lifts")
public class Lift extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String serialNumber;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(length = 100)
    private String manufacturer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LiftStatus status = LiftStatus.CREATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_organization_id")
    private Organization manufacturerOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_organization_id")
    private Organization serviceOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "management_organization_id")
    private Organization managementOrganization;

    @JsonIgnore
    @OrderBy("eventAt DESC")
    @OneToMany(mappedBy = "lift")
    private Set<LiftEvent> events = new HashSet<>();

    @JsonIgnore
    @OrderBy("requestedAt DESC")
    @OneToMany(mappedBy = "lift")
    private Set<Maintenance> maintenances = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "lift")
    private Set<Document> documents = new HashSet<>();
}
