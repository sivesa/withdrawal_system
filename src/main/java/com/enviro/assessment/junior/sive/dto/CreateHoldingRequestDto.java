package com.enviro.assessment.junior.sive.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Inbound payload for POST /api/investors/{id}/holdings, used when the admin gives a new investor an opening balance. */
public class CreateHoldingRequestDto {

    @NotNull(message = "productId is required")
    private Long productId;

    @NotNull(message = "balance is required")
    @DecimalMin(value = "0.00", message = "balance cannot be negative")
    private BigDecimal balance;

    public CreateHoldingRequestDto() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
