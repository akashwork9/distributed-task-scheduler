package com.dts.scheduler.repository;

import com.dts.scheduler.entity.Worker;
import com.dts.scheduler.entity.WorkerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    Optional<Worker> findByWorkerId(String workerId);
    List<Worker> findByStatus(WorkerStatus status);

    @Query("SELECT w FROM Worker w WHERE w.status = 'ACTIVE' AND w.lastHeartbeat <= :threshold")
    List<Worker> findStaleWorkers(@Param("threshold") Instant threshold);

    long countByStatus(WorkerStatus status);
}
