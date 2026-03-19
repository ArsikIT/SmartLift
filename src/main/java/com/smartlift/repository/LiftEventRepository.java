package com.smartlift.repository;

import com.smartlift.model.LiftEvent;
import com.smartlift.model.LiftEventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiftEventRepository extends JpaRepository<LiftEvent, Long> {

    @EntityGraph(attributePaths = {"lift", "performedBy"})
    List<LiftEvent> findAllByOrderByEventAtDesc();

    @EntityGraph(attributePaths = {"lift", "performedBy"})
    List<LiftEvent> findAllByLiftIdOrderByEventAtDesc(Long liftId);

    List<LiftEvent> findAllByType(LiftEventType type);

    @EntityGraph(attributePaths = {"lift", "performedBy"})
    Optional<LiftEvent> findDetailedById(Long id);

    Optional<LiftEvent> findTopByLiftIdOrderByEventAtDescCreatedAtDesc(Long liftId);
}
