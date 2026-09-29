package com.dts.scheduler.service;

import com.dts.scheduler.dto.worker.WorkerResponse;
import com.dts.scheduler.entity.Worker;
import com.dts.scheduler.entity.WorkerStatus;
import com.dts.scheduler.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerService {

    private final WorkerRepository workerRepository;

    @Value("${dts.worker.id:worker-node-1}")
    private String workerId;

    @Value("${dts.worker.capacity:10}")
    private int capacity;

    @Value("${dts.worker.enabled:true}")
    private boolean workerEnabled;

    @Scheduled(fixedDelayString = "${dts.worker.heartbeat-interval-ms:10000}")
    @Transactional
    public void sendHeartbeat() {
        if (!workerEnabled) {
            return;
        }

        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            Instant now = Instant.now();

            Worker worker = workerRepository.findByWorkerId(workerId)
                    .orElseGet(() -> Worker.builder()
                            .workerId(workerId)
                            .hostname(hostname)
                            .ipAddress(hostAddress)
                            .capacity(capacity)
                            .status(WorkerStatus.ACTIVE)
                            .lastHeartbeat(now)
                            .activeJobs(0)
                            .build());

            worker.setHostname(hostname);
            worker.setIpAddress(hostAddress);
            worker.setLastHeartbeat(now);
            worker.setStatus(WorkerStatus.ACTIVE);
            workerRepository.save(worker);

            log.trace("Sent heartbeat for worker [{}] on host [{}]", workerId, hostname);
        } catch (Exception e) {
            log.error("Failed to send worker heartbeat: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<WorkerResponse> getAllWorkers() {
        return workerRepository.findAll().stream()
                .map(this::mapToWorkerResponse)
                .toList();
    }

    private WorkerResponse mapToWorkerResponse(Worker worker) {
        return WorkerResponse.builder()
                .id(worker.getId())
                .workerId(worker.getWorkerId())
                .hostname(worker.getHostname())
                .ipAddress(worker.getIpAddress())
                .status(worker.getStatus())
                .lastHeartbeat(worker.getLastHeartbeat())
                .activeJobs(worker.getActiveJobs())
                .capacity(worker.getCapacity())
                .registeredAt(worker.getRegisteredAt())
                .build();
    }
}
