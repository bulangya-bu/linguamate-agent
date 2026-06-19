package com.example.javabigwork.report.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.report.dto.LearningReportView;
import com.example.javabigwork.report.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ApiResponse<LearningReportView> summary() {
        return ApiResponse.ok(reportService.summary());
    }
}
