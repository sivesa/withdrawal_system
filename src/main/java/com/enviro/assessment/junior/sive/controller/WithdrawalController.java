package com.enviro.assessment.junior.sive.controller;

import com.enviro.assessment.junior.sive.dto.WithdrawalRequestDto;
import com.enviro.assessment.junior.sive.dto.WithdrawalResponseDto;
import com.enviro.assessment.junior.sive.entity.WithdrawalStatus;
import com.enviro.assessment.junior.sive.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/withdrawals")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @PostMapping
    public ResponseEntity<WithdrawalResponseDto> createWithdrawal(@Valid @RequestBody WithdrawalRequestDto request) {
        WithdrawalResponseDto response = withdrawalService.processWithdrawal(request);

        if (response.getStatus() == WithdrawalStatus.SUCCESS) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WithdrawalResponseDto>> getHistory(
            @RequestParam(required = false) Long investorId,
            @RequestParam(required = false) WithdrawalStatus status) {
        return ResponseEntity.ok(withdrawalService.getHistory(investorId, status));
    }
}
