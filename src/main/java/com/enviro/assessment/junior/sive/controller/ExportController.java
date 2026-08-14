package com.enviro.assessment.junior.sive.controller;

import com.enviro.assessment.junior.sive.entity.WithdrawalStatus;
import com.enviro.assessment.junior.sive.service.CsvExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final CsvExportService csvExportService;

    public ExportController(CsvExportService csvExportService) {
        this.csvExportService = csvExportService;
    }

    @GetMapping("/portfolio")
    public ResponseEntity<byte[]> exportPortfolio(@RequestParam(required = false) Long investorId) {
        String csv = csvExportService.exportPortfolioCsv(investorId);
        String filename = "portfolio_export_" + LocalDate.now() + ".csv";
        return csvResponse(csv, filename);
    }

    @GetMapping("/withdrawals")
    public ResponseEntity<byte[]> exportWithdrawals(
            @RequestParam(required = false) Long investorId,
            @RequestParam(required = false) WithdrawalStatus status) {
        String csv = csvExportService.exportWithdrawalsCsv(investorId, status);
        String filename = "withdrawal_history_export_" + LocalDate.now() + ".csv";
        return csvResponse(csv, filename);
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }
}
