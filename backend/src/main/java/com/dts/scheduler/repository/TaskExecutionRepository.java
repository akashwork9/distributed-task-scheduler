package com.dts.scheduler.repository;

import com.dts.scheduler.entity.ExecutionStatus;
import com.dts.scheduler.entity.TaskExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {

    Optional<TaskExecution> findByExecutionId(String executionId);

    Page<TaskExecution> findByTaskIdOrderByCreatedAtDesc(Long taskId, Pageable pageable);

    Page<TaskExecution> findByTaskIdAndStatusOrderByCreatedAtDesc(Long taskId, ExecutionStatus status, Pageable pageable);

    Page<TaskExecution> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(e) > 0 FROM TaskExecution e WHERE e.task.id = :taskId AND e.status IN ('QUEUED', 'RUNNING')")
    boolean hasActiveExecution(@Param("taskId") Long taskId);

    @Modifying
    @Query("UPDATE TaskExecution e SET e.status = :newStatus, e.startedAt = :startedAt, e.workerId = :workerId WHERE e.executionId = :executionId AND e.status = :currentStatus")
    int claimExecution(
            @Param("executionId") String executionId,
            @Param("currentStatus") ExecutionStatus currentStatus,
            @Param("newStatus") ExecutionStatus newStatus,
            @Param("startedAt") Instant startedAt,
            @Param("workerId") String workerId
    );

    @Query("SELECT e FROM TaskExecution e WHERE e.status = 'RUNNING' AND e.startedAt <= :threshold")
    List<TaskExecution> findZombieExecutions(@Param("threshold") Instant threshold);

    long countByStatus(ExecutionStatus status);

    @Query("SELECT COUNT(e) FROM TaskExecution e WHERE e.task.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(e) FROM TaskExecution e WHERE e.task.user.id = :userId AND e.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ExecutionStatus status);
}
