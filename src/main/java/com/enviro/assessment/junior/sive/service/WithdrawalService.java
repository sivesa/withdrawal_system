package com.enviro.assessment.junior.sive.service;

import com.enviro.assessment.junior.sive.dto.WithdrawalRequestDto;
import com.enviro.assessment.junior.sive.dto.WithdrawalResponseDto;
import com.enviro.assessment.junior.sive.entity.*;
import com.enviro.assessment.junior.sive.exception.BusinessRuleException;
import com.enviro.assessment.junior.sive.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.sive.repository.HoldingRepository;
import com.enviro.assessment.junior.sive.repository.InvestorRepository;
import com.enviro.assessment.junior.sive.repository.WithdrawalNoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WithdrawalService {

    private static final BigDecimal NINETY_PERCENT = new BigDecimal("0.90");
    private static final int RETIREMENT_MIN_AGE = 65;

    private final InvestorRepository investorRepository;
    private final HoldingRepository holdingRepository;
    private final WithdrawalNoticeRepository withdrawalNoticeRepository;

    public WithdrawalService(InvestorRepository investorRepository,
                             HoldingRepository holdingRepository,
                             WithdrawalNoticeRepository withdrawalNoticeRepository) {
        this.investorRepository = investorRepository;
        this.holdingRepository = holdingRepository;
        this.withdrawalNoticeRepository = withdrawalNoticeRepository;
    }

    @Transactional
    public WithdrawalResponseDto processWithdrawal(WithdrawalRequestDto request) {
        Investor investor = investorRepository.findById(request.getInvestorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Investor not found with id: " + request.getInvestorId()));

        Holding holding = holdingRepository.findById(request.getHoldingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Holding not found with id: " + request.getHoldingId()));

        if (!holding.getInvestor().getId().equals(investor.getId())) {
            throw new BusinessRuleException(
                    "Holding " + holding.getId() + " does not belong to investor " + investor.getId());
        }

        String rejectionReason = evaluateBusinessRules(investor, holding, request.getAmount());

        WithdrawalNotice notice = new WithdrawalNotice();
        notice.setInvestor(investor);
        notice.setHolding(holding);
        notice.setRequestedAmount(request.getAmount());
        notice.setCreatedAt(LocalDateTime.now());

        if (rejectionReason != null) {
            notice.setStatus(WithdrawalStatus.REJECTED);
            notice.setReason(rejectionReason);
            notice.setBalanceAfter(null);
        } else {
            BigDecimal newBalance = holding.getBalance().subtract(request.getAmount());
            holding.setBalance(newBalance);
            holdingRepository.save(holding);

            notice.setStatus(WithdrawalStatus.SUCCESS);
            notice.setReason(null);
            notice.setBalanceAfter(newBalance);
        }

        WithdrawalNotice saved = withdrawalNoticeRepository.save(notice);
        return toResponseDto(saved);
    }

    String evaluateBusinessRules(Investor investor, Holding holding, BigDecimal amount) {
        if (holding.getProduct().getType() == ProductType.RETIREMENT_ANNUITY
                && investor.getAge() <= RETIREMENT_MIN_AGE) {
            return String.format(
                    "Retirement withdrawals are only allowed for investors older than %d (investor is %d).",
                    RETIREMENT_MIN_AGE, investor.getAge());
        }

        if (amount.compareTo(holding.getBalance()) > 0) {
            return String.format(
                    "Withdrawal amount of R%s exceeds available balance of R%s.",
                    amount.setScale(2, RoundingMode.HALF_UP), holding.getBalance().setScale(2, RoundingMode.HALF_UP));
        }

        BigDecimal maxAllowed = holding.getBalance().multiply(NINETY_PERCENT).setScale(2, RoundingMode.DOWN);
        if (amount.compareTo(maxAllowed) > 0) {
            return String.format(
                    "Withdrawal amount of R%s exceeds 90%% of available balance. Maximum allowed is R%s.",
                    amount.setScale(2, RoundingMode.HALF_UP), maxAllowed);
        }

        return null;
    }

    /**
     * @Transactional(readOnly = true): same reasoning as PortfolioService - with
     * spring.jpa.open-in-view=false, toResponseDto()'s calls to
     * notice.getInvestor().getFullName() and notice.getHolding().getProduct().getName()
     * (both LAZY associations) need an open session, which only exists for the
     * duration of this method if it's transactional.
     */
    @Transactional(readOnly = true)
    public List<WithdrawalResponseDto> getHistory(Long investorId, WithdrawalStatus status) {
        List<WithdrawalNotice> notices;
        if (investorId != null && status != null) {
            notices = withdrawalNoticeRepository.findByInvestorIdAndStatusOrderByCreatedAtDesc(investorId, status);
        } else if (investorId != null) {
            notices = withdrawalNoticeRepository.findByInvestorIdOrderByCreatedAtDesc(investorId);
        } else if (status != null) {
            notices = withdrawalNoticeRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            notices = withdrawalNoticeRepository.findAllByOrderByCreatedAtDesc();
        }
        return notices.stream().map(this::toResponseDto).collect(Collectors.toList());
    }

    private WithdrawalResponseDto toResponseDto(WithdrawalNotice notice) {
        WithdrawalResponseDto dto = new WithdrawalResponseDto();
        dto.setId(notice.getId());
        dto.setInvestorId(notice.getInvestor().getId());
        dto.setInvestorName(notice.getInvestor().getFullName());
        dto.setHoldingId(notice.getHolding().getId());
        dto.setProductName(notice.getHolding().getProduct().getName());
        dto.setRequestedAmount(notice.getRequestedAmount());
        dto.setStatus(notice.getStatus());
        dto.setReason(notice.getReason());
        dto.setBalanceAfter(notice.getBalanceAfter());
        dto.setCreatedAt(notice.getCreatedAt());
        return dto;
    }
}