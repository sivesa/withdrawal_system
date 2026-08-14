package com.enviro.assessment.junior.sive.dto;

import java.util.List;

public class PortfolioResponseDto {

    private InvestorDto investor;
    private List<HoldingDto> holdings;

    public PortfolioResponseDto() {
    }

    public PortfolioResponseDto(InvestorDto investor, List<HoldingDto> holdings) {
        this.investor = investor;
        this.holdings = holdings;
    }

    public InvestorDto getInvestor() {
        return investor;
    }

    public void setInvestor(InvestorDto investor) {
        this.investor = investor;
    }

    public List<HoldingDto> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<HoldingDto> holdings) {
        this.holdings = holdings;
    }
}
