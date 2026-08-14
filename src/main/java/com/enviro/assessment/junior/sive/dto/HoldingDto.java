package com.enviro.assessment.junior.sive.dto;

import com.enviro.assessment.junior.sive.entity.ProductType;

import java.math.BigDecimal;

public class HoldingDto {

    private Long holdingId;
    private Long productId;
    private String productName;
    private ProductType productType;
    private BigDecimal balance;
    private BigDecimal maxWithdrawable;

    public HoldingDto() {
    }

    public HoldingDto(Long holdingId, Long productId, String productName, ProductType productType,
                       BigDecimal balance, BigDecimal maxWithdrawable) {
        this.holdingId = holdingId;
        this.productId = productId;
        this.productName = productName;
        this.productType = productType;
        this.balance = balance;
        this.maxWithdrawable = maxWithdrawable;
    }

    public Long getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(Long holdingId) {
        this.holdingId = holdingId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getMaxWithdrawable() {
        return maxWithdrawable;
    }

    public void setMaxWithdrawable(BigDecimal maxWithdrawable) {
        this.maxWithdrawable = maxWithdrawable;
    }
}
