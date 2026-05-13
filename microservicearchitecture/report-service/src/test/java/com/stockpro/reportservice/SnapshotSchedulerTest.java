package com.stockpro.reportservice;

import com.stockpro.reportservice.service.SnapshotScheduler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SnapshotSchedulerTest {

    private final SnapshotScheduler scheduler = new SnapshotScheduler();

    @Test
    void scheduledJobs_RunWithoutExternalDependencies() {
        assertDoesNotThrow(() -> scheduler.takeDailySnapshot());
        assertDoesNotThrow(() -> scheduler.checkLowStock());
        assertDoesNotThrow(() -> scheduler.checkOverduePOs());
    }
}
