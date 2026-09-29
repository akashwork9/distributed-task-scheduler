package com.dts.scheduler.repository;

import com.dts.scheduler.entity.Task;
import com.dts.scheduler.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Page<Task> findByUserId(Long userId, Pageable pageable);

    Page<Task> findByUserIdAndStatus(Long userId, TaskStatus status, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    @Query(value = "SELECT * FROM tasks WHERE status = 'ACTIVE' AND enabled = true AND next_run_at <= :now ORDER BY next_run_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Task> findEligibleTasksForScheduling(@Param("now") Instant now, @Param("limit") int limit);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, TaskStatus status);

    long countByStatus(TaskStatus status);
}
