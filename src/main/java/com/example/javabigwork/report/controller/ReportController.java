package com.example.javabigwork.report.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.report.dto.LearningReportView;
import com.example.javabigwork.report.service.ReportService;
import com.example.javabigwork.report.service.LearningJournalService;
import com.example.javabigwork.report.dto.LearningJournalView;
import com.example.javabigwork.report.dto.GrowthEvidenceView;
import com.example.javabigwork.report.service.GrowthEvidenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class ReportController {
    private final ReportService reportService;
    private final LearningJournalService journalService;
    private final GrowthEvidenceService growth;

    public ReportController(ReportService reportService, LearningJournalService journalService,GrowthEvidenceService growth) {
        this.reportService = reportService;
        this.journalService = journalService;
        this.growth=growth;
    }

    @GetMapping("/summary")
    public ApiResponse<LearningReportView> summary() {
        return ApiResponse.ok(reportService.summary());
    }

    @GetMapping("/journal")
    public ApiResponse<LearningJournalView> journal() { return ApiResponse.ok(journalService.journal()); }
    @GetMapping("/growth") public ApiResponse<GrowthEvidenceView> growth() {return ApiResponse.ok(growth.evidence());}
}
