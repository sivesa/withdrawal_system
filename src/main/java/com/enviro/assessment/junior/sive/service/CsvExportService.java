package com.enviro.assessment.junior.sive.service;

import com.enviro.assessment.junior.sive.entity.Holding;
import com.enviro.assessment.junior.sive.entity.WithdrawalNotice;
import com.enviro.assessment.junior.sive.entity.WithdrawalStatus;
import com.enviro.assessment.junior.sive.repository.HoldingRepository;
import com.enviro.assessment.junior.sive.repository.WithdrawalNoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CsvExportService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final HoldingRepository holdingRepository;
    private final WithdrawalNoticeRepository withdrawalNoticeRepository;

    public CsvExportService(HoldingRepository holdingRepository, WithdrawalNoticeRepository withdrawalNoticeRepository) {
        this.holdingRepository = holdingRepository;
        this.withdrawalNoticeRepository = withdrawalNoticeRepository;
    }

    /**
     * @Transactional(readOnly = true): same "no Session" issue as the other
     * services - h.getInvestor()/.getProduct() are LAZY and are read here well
     * after the initial repository query would otherwise have closed its session.
     */
    @Transactional(readOnly = true)
    public String exportPortfolioCsv(Long investorId) {
        List<Holding> holdings = investorId != null
                ? holdingRepository.findByInvestorId(investorId)
                : holdingRepository.findAll();

        holdings = holdings.stream()
                .sorted(Comparator.comparing(h -> h.getInvestor().getFullName()))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("InvestorId,InvestorName,Email,Age,ProductId,ProductName,ProductType,Balance,MaxWithdrawable90pct\n");

        for (Holding h : holdings) {
            java.math.BigDecimal maxWithdrawable = h.getBalance()
                    .multiply(new java.math.BigDecimal("0.90"))
                    .setScale(2, java.math.RoundingMode.DOWN);

            sb.append(csvRow(
                    h.getInvestor().getId(),
                    h.getInvestor().getFullName(),
                    h.getInvestor().getEmail(),
                    h.getInvestor().getAge(),
                    h.getProduct().getId(),
                    h.getProduct().getName(),
                    h.getProduct().getType(),
                    h.getBalance(),
                    maxWithdrawable
            ));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportWithdrawalsCsv(Long investorId, WithdrawalStatus status) {
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

        StringBuilder sb = new StringBuilder();
        sb.append("WithdrawalId,InvestorId,InvestorName,ProductName,RequestedAmount,Status,Reason,BalanceAfter,CreatedAt\n");

        for (WithdrawalNotice n : notices) {
            sb.append(csvRow(
                    n.getId(),
                    n.getInvestor().getId(),
                    n.getInvestor().getFullName(),
                    n.getHolding().getProduct().getName(),
                    n.getRequestedAmount(),
                    n.getStatus(),
                    n.getReason() == null ? "" : n.getReason(),
                    n.getBalanceAfter() == null ? "" : n.getBalanceAfter().toString(),
                    n.getCreatedAt().format(TIMESTAMP_FORMAT)
            ));
        }
        return sb.toString();
    }

    private String csvRow(Object... values) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            row.append(escape(values[i]));
            if (i < values.length - 1) {
                row.append(',');
            }
        }
        row.append('\n');
        return row.toString();
    }

    private String escape(Object value) {
        String s = String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}