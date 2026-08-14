package com.enviro.assessment.junior.sive.service;

import com.enviro.assessment.junior.sive.dto.WithdrawalRequestDto;
import com.enviro.assessment.junior.sive.dto.WithdrawalResponseDto;
import com.enviro.assessment.junior.sive.entity.*;
import com.enviro.assessment.junior.sive.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.sive.repository.HoldingRepository;
import com.enviro.assessment.junior.sive.repository.InvestorRepository;
import com.enviro.assessment.junior.sive.repository.WithdrawalNoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WithdrawalService, covering the three non-negotiable
 * business rules plus the structural error paths. Repositories are mocked
 * with Mockito so these tests run without a Spring context or database,
 * keeping them fast and focused purely on business logic.
 */
@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private InvestorRepository investorRepository;
    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private WithdrawalNoticeRepository withdrawalNoticeRepository;

    @InjectMocks
    private WithdrawalService withdrawalService;

    private Product retirementProduct;
    private Product savingsProduct;

    @BeforeEach
    void setUp() {
        retirementProduct = new Product("Retirement Annuity", ProductType.RETIREMENT_ANNUITY);
        retirementProduct.setId(1L);

        savingsProduct = new Product("Savings Plan", ProductType.SAVINGS_PLAN);
        savingsProduct.setId(2L);
    }

    private Investor investorAged(int age) {
        Investor investor = new Investor("Test Investor", "test@example.com", LocalDate.now().minusYears(age));
        investor.setId(1L);
        return investor;
    }

    private Holding holdingOf(Investor investor, Product product, String balance) {
        Holding holding = new Holding(investor, product, new BigDecimal(balance));
        holding.setId(1L);
        return holding;
    }

    // ---------- Rule 1: retirement withdrawals require age > 65 ----------

    @Test
    void rejectsRetirementWithdrawal_whenInvestorIs65OrYounger() {
        Investor investor = investorAged(65);
        Holding holding = holdingOf(investor, retirementProduct, "100000.00");

        String reason = withdrawalService.evaluateBusinessRules(investor, holding, new BigDecimal("1000"));

        assertNotNull(reason, "A 65 year old should NOT be allowed to withdraw from a retirement product");
        assertTrue(reason.contains("older than 65"));
    }

    @Test
    void allowsRetirementWithdrawal_whenInvestorIsOlderThan65() {
        Investor investor = investorAged(70);
        Holding holding = holdingOf(investor, retirementProduct, "100000.00");

        String reason = withdrawalService.evaluateBusinessRules(investor, holding, new BigDecimal("1000"));

        assertNull(reason, "A 70 year old should be allowed to withdraw from a retirement product (within balance rules)");
    }

    @Test
    void ageRuleDoesNotApply_toNonRetirementProducts() {
        Investor investor = investorAged(30);
        Holding holding = holdingOf(investor, savingsProduct, "50000.00");

        String reason = withdrawalService.evaluateBusinessRules(investor, holding, new BigDecimal("1000"));

        assertNull(reason, "The age rule should only apply to RETIREMENT_ANNUITY products");
    }

    // ---------- Rule 2: amount must not exceed available balance ----------

    @Test
    void rejectsWithdrawal_whenAmountExceedsBalance() {
        Investor investor = investorAged(40);
        Holding holding = holdingOf(investor, savingsProduct, "10000.00");

        String reason = withdrawalService.evaluateBusinessRules(investor, holding, new BigDecimal("15000.00"));

        assertNotNull(reason);
        assertTrue(reason.contains("exceeds available balance"));
    }

    // ---------- Rule 3: amount must not exceed 90% of available balance ----------

    @Test
    void rejectsWithdrawal_whenAmountExceedsNinetyPercentOfBalance() {
        Investor investor = investorAged(40);
        Holding holding = holdingOf(investor, savingsProduct, "10000.00");

        // 95% of balance - within balance, but over the 90% cap
        String reason = withdrawalService.evaluateBusinessRules(investor, holding, new BigDecimal("9500.00"));

        assertNotNull(reason);
        assertTrue(reason.contains("90%"));
    }

    @Test
    void allowsWithdrawal_whenAmountIsExactlyNinetyPercentOfBalance() {
        Investor investor = investorAged(40);
        Holding holding = holdingOf(investor, savingsProduct, "10000.00");

        String reason = withdrawalService.evaluateBusinessRules(investor, holding, new BigDecimal("9000.00"));

        assertNull(reason, "Exactly 90% of the balance should be allowed (not exceeding the cap)");
    }

    @Test
    void allowsWithdrawal_whenAllRulesPass() {
        Investor investor = investorAged(40);
        Holding holding = holdingOf(investor, savingsProduct, "10000.00");

        String reason = withdrawalService.evaluateBusinessRules(investor, holding, new BigDecimal("5000.00"));

        assertNull(reason);
    }

    // ---------- End-to-end processWithdrawal: persistence + response shape ----------

    @Test
    void processWithdrawal_persistsSuccessAndDeductsBalance() {
        Investor investor = investorAged(40);
        Holding holding = holdingOf(investor, savingsProduct, "10000.00");

        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(holdingRepository.findById(1L)).thenReturn(Optional.of(holding));
        when(withdrawalNoticeRepository.save(any(WithdrawalNotice.class))).thenAnswer(inv -> {
            WithdrawalNotice n = inv.getArgument(0);
            n.setId(99L);
            return n;
        });

        WithdrawalRequestDto request = new WithdrawalRequestDto();
        request.setInvestorId(1L);
        request.setHoldingId(1L);
        request.setAmount(new BigDecimal("2000.00"));

        WithdrawalResponseDto response = withdrawalService.processWithdrawal(request);

        assertEquals(WithdrawalStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("8000.00"), response.getBalanceAfter());
        assertEquals(new BigDecimal("8000.00"), holding.getBalance(), "Holding balance should be reduced");
    }

    @Test
    void processWithdrawal_persistsRejectionWithoutChangingBalance() {
        Investor investor = investorAged(40);
        Holding holding = holdingOf(investor, savingsProduct, "10000.00");

        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(holdingRepository.findById(1L)).thenReturn(Optional.of(holding));
        when(withdrawalNoticeRepository.save(any(WithdrawalNotice.class))).thenAnswer(inv -> {
            WithdrawalNotice n = inv.getArgument(0);
            n.setId(100L);
            return n;
        });

        WithdrawalRequestDto request = new WithdrawalRequestDto();
        request.setInvestorId(1L);
        request.setHoldingId(1L);
        request.setAmount(new BigDecimal("20000.00")); // exceeds balance

        WithdrawalResponseDto response = withdrawalService.processWithdrawal(request);

        assertEquals(WithdrawalStatus.REJECTED, response.getStatus());
        assertNotNull(response.getReason());
        assertNull(response.getBalanceAfter());
        assertEquals(new BigDecimal("10000.00"), holding.getBalance(), "Balance must be untouched on rejection");
    }

    @Test
    void processWithdrawal_throwsNotFound_whenInvestorDoesNotExist() {
        when(investorRepository.findById(99L)).thenReturn(Optional.empty());

        WithdrawalRequestDto request = new WithdrawalRequestDto();
        request.setInvestorId(99L);
        request.setHoldingId(1L);
        request.setAmount(BigDecimal.TEN);

        assertThrows(ResourceNotFoundException.class, () -> withdrawalService.processWithdrawal(request));
    }
}
