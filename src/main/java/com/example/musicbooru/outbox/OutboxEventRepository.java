package com.example.musicbooru.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusAndAttemptsLessThan(OutboxStatus status, int attemptsIsLessThan);
}
