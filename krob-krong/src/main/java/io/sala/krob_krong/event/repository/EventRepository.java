package io.sala.krob_krong.event.repository;

import io.sala.krob_krong.event.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID>, JpaSpecificationExecutor<EventEntity> {
}
