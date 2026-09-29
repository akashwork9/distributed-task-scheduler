package com.dts.scheduler.entity;

public enum ExecutionStatus {
    SCHEDULED,
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    CANCELLED
}
