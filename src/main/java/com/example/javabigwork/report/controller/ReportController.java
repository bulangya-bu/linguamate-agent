package com.example.javabigwork.report.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.report.dto.LearningReportView;
import com.example.javabigwork.report.service.ReportService;
import com.example.javabigwork.report.service.LearningJournalService;
import com.example.javabigwork.report.dto.LearningJournalView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class ReportController {
    private final ReportService reportService;
    private final LearningJournalService journalService;

    public ReportController(ReportService reportService, LearningJournalService journalService) {
        this.reportService = reportService;
        this.journalService = journalService;
    }

    @GetMapping("/summary")
    public ApiResponse<LearningReportView> summary() {
        return ApiResponse.ok(reportService.summary());
    }

    @GetMapping("/journal")
    public ApiResponse<LearningJournalView> journal() { return ApiResponse.ok(journalService.journal()); }
}
