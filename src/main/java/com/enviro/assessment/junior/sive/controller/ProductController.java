package com.enviro.assessment.junior.sive.controller;

import com.enviro.assessment.junior.sive.dto.ProductDto;
import com.enviro.assessment.junior.sive.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /api/products - lists the available investment products. Used by the admin page's "add holding" dropdown. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final PortfolioService portfolioService;

    public ProductController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(portfolioService.getAllProducts());
    }
}
