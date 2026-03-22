package com.smartlift.repository;

import com.smartlift.model.LiftEvent;
import com.smartlift.model.enums.LiftEventType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiftEventRepository extends JpaRepository<LiftEvent, Long> {

    @EntityGraph(attributePaths = {"lift", "performedBy"})
    Page<LiftEvent> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"lift", "performedBy"})
    Page<LiftEvent> findAllByLiftId(Long liftId, Pageable pageable);

    Page<LiftEvent> findAllByType(LiftEventType type, Pageable pageable);

    @EntityGraph(attributePaths = {"lift", "performedBy"})
    Optional<LiftEvent> findDetailedById(Long id);

    Optional<LiftEvent> findTopByLiftIdOrderByEventAtDescCreatedAtDesc(Long liftId);
}
