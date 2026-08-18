package com.enviro.assessment.junior.sive.service;

import com.enviro.assessment.junior.sive.entity.*;
import com.enviro.assessment.junior.sive.repository.HoldingRepository;
import com.enviro.assessment.junior.sive.repository.WithdrawalNoticeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CsvExportService: correct header rows, correct choice of
 * repository query for each investorId/status filter combination, correct
 * field values (including the computed 90%-of-balance column), and CSV
 * escaping of values containing commas/quotes. Repositories are mocked with
 * Mockito - no Spring context or database needed.
 */
@ExtendWith(MockitoExtension.class)
class CsvExportServiceTest {

    private static final String PORTFOLIO_HEADER =
            "InvestorId,InvestorName,Email,Age,ProductId,ProductName,ProductType,Balance,MaxWithdrawable90pct";
    private static final String WITHDRAWALS_HEADER =
            "WithdrawalId,InvestorId,InvestorName,ProductName,RequestedAmount,Status,Reason,BalanceAfter,CreatedAt";

    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private WithdrawalNoticeRepository withdrawalNoticeRepository;

    @InjectMocks
    private CsvExportService csvExportService;

    private Investor investorOf(long id, String name, String email, int age) {
        Investor investor = new Investor(name, email, LocalDate.now().minusYears(age));
        investor.setId(id);
        return investor;
    }

    private Product productOf(long id, String name, ProductType type) {
        Product product = new Product(name, type);
        product.setId(id);
        return product;
    }

    private Holding holdingOf(long id, Investor investor, Product product, String balance) {
        Holding holding = new Holding(investor, product, new BigDecimal(balance));
        holding.setId(id);
        return holding;
    }

    private WithdrawalNotice noticeOf(long id, Investor investor, Holding holding, WithdrawalStatus status) {
        WithdrawalNotice notice = new WithdrawalNotice();
        notice.setId(id);
        notice.setInvestor(investor);
        notice.setHolding(holding);
        notice.setRequestedAmount(new BigDecimal("1000.00"));
        notice.setStatus(status);
        notice.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 30, 0));
        return notice;
    }

    // =====================================================================
    // exportPortfolioCsv()
    // =====================================================================

    @Test
    void exportPortfolioCsv_returnsHeaderOnly_whenNoHoldingsExist() {
        when(holdingRepository.findAll()).thenReturn(List.of());

        String csv = csvExportService.exportPortfolioCsv(null);

        assertEquals(PORTFOLIO_HEADER + "\n", csv);
    }

    @Test
    void exportPortfolioCsv_usesFindAll_whenInvestorIdIsNull() {
        when(holdingRepository.findAll()).thenReturn(List.of());

        csvExportService.exportPortfolioCsv(null);

        verify(holdingRepository).findAll();
        verify(holdingRepository, never()).findByInvestorId(any());
    }

    @Test
    void exportPortfolioCsv_usesFindByInvestorId_whenInvestorIdProvided() {
        when(holdingRepository.findByInvestorId(1L)).thenReturn(List.of());

        csvExportService.exportPortfolioCsv(1L);

        verify(holdingRepository).findByInvestorId(1L);
        verify(holdingRepository, never()).findAll();
    }

    @Test
    void exportPortfolioCsv_includesExpectedRowWithComputedMaxWithdrawable() {
        Investor investor = investorOf(1L, "Jane Doe", "jane@example.com", 40);
        Product product = productOf(2L, "Retirement Annuity", ProductType.RETIREMENT_ANNUITY);
        Holding holding = holdingOf(3L, investor, product, "100000.00");

        when(holdingRepository.findByInvestorId(1L)).thenReturn(List.of(holding));

        String csv = csvExportService.exportPortfolioCsv(1L);
        String[] lines = csv.split("\n");

        assertEquals(2, lines.length);
        assertEquals(PORTFOLIO_HEADER, lines[0]);
        assertEquals("1,Jane Doe,jane@example.com,40,2,Retirement Annuity,RETIREMENT_ANNUITY,100000.00,90000.00", lines[1]);
    }

    @Test
    void exportPortfolioCsv_sortsRowsByInvestorFullName() {
        Investor zeta = investorOf(1L, "Zeta Ngcobo", "zeta@example.com", 40);
        Investor alpha = investorOf(2L, "Alpha Mokoena", "alpha@example.com", 45);
        Product product = productOf(3L, "Savings Plan", ProductType.SAVINGS_PLAN);

        Holding zetaHolding = holdingOf(4L, zeta, product, "1000.00");
        Holding alphaHolding = holdingOf(5L, alpha, product, "2000.00");

        // Deliberately returned out of alphabetical order to prove the service sorts them.
        when(holdingRepository.findAll()).thenReturn(List.of(zetaHolding, alphaHolding));

        String csv = csvExportService.exportPortfolioCsv(null);
        String[] lines = csv.split("\n");

        assertTrue(lines[1].startsWith("2,Alpha Mokoena"));
        assertTrue(lines[2].startsWith("1,Zeta Ngcobo"));
    }

    @Test
    void exportPortfolioCsv_includesOneRowPerHolding() {
        Investor investor = investorOf(1L, "Jane Doe", "jane@example.com", 40);
        Product savings = productOf(2L, "Savings Plan", ProductType.SAVINGS_PLAN);
        Product discretionary = productOf(3L, "Discretionary Investment", ProductType.DISCRETIONARY_INVESTMENT);

        Holding h1 = holdingOf(4L, investor, savings, "5000.00");
        Holding h2 = holdingOf(5L, investor, discretionary, "8000.00");

        when(holdingRepository.findByInvestorId(1L)).thenReturn(List.of(h1, h2));

        String csv = csvExportService.exportPortfolioCsv(1L);

        assertEquals(3, csv.split("\n").length); // header + 2 holdings
    }

    // =====================================================================
    // exportWithdrawalsCsv() - repository selection per filter combination
    // =====================================================================

    @Test
    void exportWithdrawalsCsv_returnsHeaderOnly_whenNoNoticesExist() {
        when(withdrawalNoticeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        String csv = csvExportService.exportWithdrawalsCsv(null, null);

        assertEquals(WITHDRAWALS_HEADER + "\n", csv);
    }

    @Test
    void exportWithdrawalsCsv_usesFindAll_whenNoFiltersGiven() {
        when(withdrawalNoticeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        csvExportService.exportWithdrawalsCsv(null, null);

        verify(withdrawalNoticeRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void exportWithdrawalsCsv_usesInvestorFilter_whenOnlyInvestorIdGiven() {
        when(withdrawalNoticeRepository.findByInvestorIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        csvExportService.exportWithdrawalsCsv(1L, null);

        verify(withdrawalNoticeRepository).findByInvestorIdOrderByCreatedAtDesc(1L);
        verify(withdrawalNoticeRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void exportWithdrawalsCsv_usesStatusFilter_whenOnlyStatusGiven() {
        when(withdrawalNoticeRepository.findByStatusOrderByCreatedAtDesc(WithdrawalStatus.REJECTED))
                .thenReturn(List.of());

        csvExportService.exportWithdrawalsCsv(null, WithdrawalStatus.REJECTED);

        verify(withdrawalNoticeRepository).findByStatusOrderByCreatedAtDesc(WithdrawalStatus.REJECTED);
    }

    @Test
    void exportWithdrawalsCsv_usesCombinedFilter_whenBothInvestorIdAndStatusGiven() {
        when(withdrawalNoticeRepository.findByInvestorIdAndStatusOrderByCreatedAtDesc(1L, WithdrawalStatus.SUCCESS))
                .thenReturn(List.of());

        csvExportService.exportWithdrawalsCsv(1L, WithdrawalStatus.SUCCESS);

        verify(withdrawalNoticeRepository)
                .findByInvestorIdAndStatusOrderByCreatedAtDesc(1L, WithdrawalStatus.SUCCESS);
    }

    // =====================================================================
    // exportWithdrawalsCsv() - row content
    // =====================================================================

    @Test
    void exportWithdrawalsCsv_leavesReasonBlank_forSuccessfulWithdrawal() {
        Investor investor = investorOf(1L, "Jane Doe", "jane@example.com", 40);
        Product product = productOf(2L, "Savings Plan", ProductType.SAVINGS_PLAN);
        Holding holding = holdingOf(3L, investor, product, "10000.00");
        WithdrawalNotice notice = noticeOf(4L, investor, holding, WithdrawalStatus.SUCCESS);
        notice.setBalanceAfter(new BigDecimal("9000.00"));

        when(withdrawalNoticeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(notice));

        String csv = csvExportService.exportWithdrawalsCsv(null, null);
        String row = csv.split("\n")[1];

        assertEquals("4,1,Jane Doe,Savings Plan,1000.00,SUCCESS,,9000.00,2026-01-15 10:30:00", row);
    }

    @Test
    void exportWithdrawalsCsv_leavesBalanceAfterBlank_forRejectedWithdrawal() {
        Investor investor = investorOf(1L, "Jane Doe", "jane@example.com", 40);
        Product product = productOf(2L, "Savings Plan", ProductType.SAVINGS_PLAN);
        Holding holding = holdingOf(3L, investor, product, "10000.00");
        WithdrawalNotice notice = noticeOf(4L, investor, holding, WithdrawalStatus.REJECTED);
        notice.setReason("Amount exceeds available balance.");
        notice.setBalanceAfter(null);

        when(withdrawalNoticeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(notice));

        String csv = csvExportService.exportWithdrawalsCsv(null, null);
        String row = csv.split("\n")[1];

        assertEquals(
                "4,1,Jane Doe,Savings Plan,1000.00,REJECTED,Amount exceeds available balance.,,2026-01-15 10:30:00",
                row);
    }

    @Test
    void exportWithdrawalsCsv_quotesFieldsContainingCommas() {
        Investor investor = investorOf(1L, "Jane Doe", "jane@example.com", 40);
        Product product = productOf(2L, "Savings Plan", ProductType.SAVINGS_PLAN);
        Holding holding = holdingOf(3L, investor, product, "10000.00");
        WithdrawalNotice notice = noticeOf(4L, investor, holding, WithdrawalStatus.REJECTED);
        notice.setReason("Amount exceeds available balance, please try a smaller amount.");

        when(withdrawalNoticeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(notice));

        String csv = csvExportService.exportWithdrawalsCsv(null, null);

        assertTrue(csv.contains("\"Amount exceeds available balance, please try a smaller amount.\""),
                "A reason containing a comma must be wrapped in quotes so it stays one CSV field");
    }

    @Test
    void exportWithdrawalsCsv_escapesEmbeddedDoubleQuotes() {
        Investor investor = investorOf(1L, "Jane Doe", "jane@example.com", 40);
        Product product = productOf(2L, "Savings Plan", ProductType.SAVINGS_PLAN);
        Holding holding = holdingOf(3L, investor, product, "10000.00");
        WithdrawalNotice notice = noticeOf(4L, investor, holding, WithdrawalStatus.REJECTED);
        notice.setReason("Investor said \"no\" to further withdrawals");

        when(withdrawalNoticeRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(notice));

        String csv = csvExportService.exportWithdrawalsCsv(null, null);

        assertTrue(csv.contains("\"Investor said \"\"no\"\" to further withdrawals\""),
                "Embedded double quotes must be doubled per standard CSV escaping");
    }

    @Test
    void exportWithdrawalsCsv_includesOneRowPerNotice() {
        Investor investor = investorOf(1L, "Jane Doe", "jane@example.com", 40);
        Product product = productOf(2L, "Savings Plan", ProductType.SAVINGS_PLAN);
        Holding holding = holdingOf(3L, investor, product, "10000.00");

        WithdrawalNotice n1 = noticeOf(4L, investor, holding, WithdrawalStatus.SUCCESS);
        WithdrawalNotice n2 = noticeOf(5L, investor, holding, WithdrawalStatus.REJECTED);

        when(withdrawalNoticeRepository.findByInvestorIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1, n2));

        String csv = csvExportService.exportWithdrawalsCsv(1L, null);

        assertEquals(3, csv.split("\n").length); // header + 2 notices
    }
}
