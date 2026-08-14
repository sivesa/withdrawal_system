package com.enviro.assessment.junior.sive.controller;

import com.enviro.assessment.junior.sive.dto.*;
import com.enviro.assessment.junior.sive.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investors")
public class InvestorController {

    private final PortfolioService portfolioService;

    public InvestorController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    /** GET /api/investors - list all investors (used by login page + admin investor table). */
    @GetMapping
    public ResponseEntity<List<InvestorDto>> getAllInvestors() {
        return ResponseEntity.ok(portfolioService.getAllInvestors());
    }

    /** GET /api/investors/{id}/portfolio - investor details + current holdings. */
    @GetMapping("/{id}/portfolio")
    public ResponseEntity<PortfolioResponseDto> getPortfolio(@PathVariable Long id) {
        return ResponseEntity.ok(portfolioService.getPortfolio(id));
    }

    /**
     * POST /api/investors - creates a new investor. Backs the admin "Add Investor" page.
     * Returns 201 with the created investor, or 409 if the email is already in use
     * (see GlobalExceptionHandler), or 400 if fields are missing/invalid.
     */
    @PostMapping
    public ResponseEntity<InvestorDto> createInvestor(@Valid @RequestBody CreateInvestorRequestDto request) {
        InvestorDto created = portfolioService.createInvestor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/investors/{id}/holdings - gives an investor an opening holding in a product.
     * Used by the admin page to optionally seed a starting balance right after creating an investor.
     */
    @PostMapping("/{id}/holdings")
    public ResponseEntity<HoldingDto> addHolding(@PathVariable Long id, @Valid @RequestBody CreateHoldingRequestDto request) {
        HoldingDto created = portfolioService.addHolding(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
