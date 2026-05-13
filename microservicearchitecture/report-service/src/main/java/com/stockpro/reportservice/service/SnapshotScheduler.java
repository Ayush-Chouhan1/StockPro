package com.stockpro.reportservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SnapshotScheduler {

    @Autowired
    private ReportService reportService;

    // Daily snapshot at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void takeDailySnapshot() {
        log.info("Running daily inventory snapshot job");
        // In full integration, this would call warehouse-service
        // to get all stock levels and create snapshots
        // For now, logs that the job ran
        log.info("Daily snapshot job completed at midnight");
    }

    // Low stock check every 15 minutes
    @Scheduled(fixedRate = 900000)
    public void checkLowStock() {
        log.debug("Running low stock check");
        // Would trigger alerts via alert-service in full integration
    }

    // Overdue PO alert at 9:00 AM daily
    @Scheduled(cron = "0 0 9 * * *")
    public void checkOverduePOs() {
        log.info("Running overdue PO check");
        // Would call purchase-service and trigger alerts
    }
}