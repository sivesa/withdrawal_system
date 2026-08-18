package com.enviro.assessment.junior.sive.service;

import com.enviro.assessment.junior.sive.dto.*;
import com.enviro.assessment.junior.sive.entity.Holding;
import com.enviro.assessment.junior.sive.entity.Investor;
import com.enviro.assessment.junior.sive.entity.Product;
import com.enviro.assessment.junior.sive.entity.ProductType;
import com.enviro.assessment.junior.sive.exception.DuplicateResourceException;
import com.enviro.assessment.junior.sive.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.sive.repository.HoldingRepository;
import com.enviro.assessment.junior.sive.repository.InvestorRepository;
import com.enviro.assessment.junior.sive.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PortfolioService: portfolio retrieval, the DTO mapping
 * (including the 90%-of-balance calculation and its rounding direction),
 * and the admin-facing investor/holding creation flows. Repositories are
 * mocked with Mockito - no Spring context or database needed.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private InvestorRepository investorRepository;
    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private Investor investor;
    private Product product;

    @BeforeEach
    void setUp() {
        investor = new Investor("Jane Doe", "jane.doe@example.com", LocalDate.now().minusYears(40));
        investor.setId(1L);

        product = new Product("Enviro365 Savings Plan", ProductType.SAVINGS_PLAN);
        product.setId(2L);
    }

    private Holding holdingOf(Investor investor, Product product, String balance) {
        Holding holding = new Holding(investor, product, new BigDecimal(balance));
        holding.setId(3L);
        return holding;
    }

    // =====================================================================
    // getAllInvestors()
    // =====================================================================

    @Test
    void getAllInvestors_returnsMappedDtos() {
        when(investorRepository.findAll()).thenReturn(List.of(investor));

        List<InvestorDto> result = portfolioService.getAllInvestors();

        assertEquals(1, result.size());
        InvestorDto dto = result.get(0);
        assertEquals(investor.getId(), dto.getId());
        assertEquals("Jane Doe", dto.getFullName());
        assertEquals("jane.doe@example.com", dto.getEmail());
        assertEquals(40, dto.getAge());
    }

    @Test
    void getAllInvestors_returnsEmptyList_whenNoInvestorsExist() {
        when(investorRepository.findAll()).thenReturn(List.of());

        List<InvestorDto> result = portfolioService.getAllInvestors();

        assertTrue(result.isEmpty());
    }

    // =====================================================================
    // getPortfolio()
    // =====================================================================

    @Test
    void getPortfolio_returnsInvestorAndHoldings_whenInvestorExists() {
        Holding holding = holdingOf(investor, product, "10000.00");

        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(holdingRepository.findByInvestorId(1L)).thenReturn(List.of(holding));

        PortfolioResponseDto result = portfolioService.getPortfolio(1L);

        assertEquals("Jane Doe", result.getInvestor().getFullName());
        assertEquals(1, result.getHoldings().size());

        HoldingDto holdingDto = result.getHoldings().get(0);
        assertEquals(holding.getId(), holdingDto.getHoldingId());
        assertEquals(product.getId(), holdingDto.getProductId());
        assertEquals(product.getName(), holdingDto.getProductName());
        assertEquals(ProductType.SAVINGS_PLAN, holdingDto.getProductType());
        assertEquals(new BigDecimal("10000.00"), holdingDto.getBalance());
        assertEquals(new BigDecimal("9000.00"), holdingDto.getMaxWithdrawable());
    }

    @Test
    void getPortfolio_throwsNotFound_whenInvestorDoesNotExist() {
        when(investorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> portfolioService.getPortfolio(99L));
        verifyNoInteractions(holdingRepository);
    }

    @Test
    void getPortfolio_returnsEmptyHoldingsList_whenInvestorHasNoHoldings() {
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(holdingRepository.findByInvestorId(1L)).thenReturn(List.of());

        PortfolioResponseDto result = portfolioService.getPortfolio(1L);

        assertTrue(result.getHoldings().isEmpty());
    }

    @Test
    void getPortfolio_roundsMaxWithdrawableDown_notToNearest() {
        // 100.01 * 0.90 = 90.009 -> DOWN to 2dp = 90.00 (HALF_UP would give 90.01,
        // which would let an investor sneak past the true 90% cap by a cent).
        Holding holding = holdingOf(investor, product, "100.01");

        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(holdingRepository.findByInvestorId(1L)).thenReturn(List.of(holding));

        PortfolioResponseDto result = portfolioService.getPortfolio(1L);

        assertEquals(new BigDecimal("90.00"), result.getHoldings().get(0).getMaxWithdrawable());
    }

    // =====================================================================
    // createInvestor()
    // =====================================================================

    private CreateInvestorRequestDto createInvestorRequest(String fullName, String email, LocalDate dob) {
        CreateInvestorRequestDto request = new CreateInvestorRequestDto();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setDateOfBirth(dob);
        return request;
    }

    @Test
    void createInvestor_savesAndReturnsDto_whenEmailIsUnique() {
        CreateInvestorRequestDto request = createInvestorRequest(
                "Nomvula Zulu", "nomvula.zulu@example.com", LocalDate.of(1958, 3, 14));

        when(investorRepository.existsByEmailIgnoreCase("nomvula.zulu@example.com")).thenReturn(false);
        when(investorRepository.save(any(Investor.class))).thenAnswer(inv -> {
            Investor saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        InvestorDto result = portfolioService.createInvestor(request);

        assertEquals(10L, result.getId());
        assertEquals("Nomvula Zulu", result.getFullName());
        assertEquals("nomvula.zulu@example.com", result.getEmail());
        assertEquals(LocalDate.of(1958, 3, 14), result.getDateOfBirth());
        verify(investorRepository).save(any(Investor.class));
    }

    @Test
    void createInvestor_throwsDuplicate_whenEmailAlreadyExists() {
        CreateInvestorRequestDto request = createInvestorRequest(
                "Nomvula Zulu", "jane.doe@example.com", LocalDate.of(1958, 3, 14));

        when(investorRepository.existsByEmailIgnoreCase("jane.doe@example.com")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> portfolioService.createInvestor(request));
        assertTrue(ex.getMessage().contains("jane.doe@example.com"));
        verify(investorRepository, never()).save(any());
    }

    @Test
    void createInvestor_checksEmailUniquenessCaseInsensitively() {
        // existsByEmailIgnoreCase is a case-insensitive query by name; verify the
        // service passes the raw email through untouched and trusts the repository
        // method to do the case-insensitive comparison rather than lower-casing it itself.
        CreateInvestorRequestDto request = createInvestorRequest(
                "Test", "Jane.Doe@Example.com", LocalDate.of(1958, 3, 14));

        when(investorRepository.existsByEmailIgnoreCase("Jane.Doe@Example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> portfolioService.createInvestor(request));
        verify(investorRepository).existsByEmailIgnoreCase("Jane.Doe@Example.com");
    }

    // =====================================================================
    // addHolding()
    // =====================================================================

    private CreateHoldingRequestDto createHoldingRequest(Long productId, String balance) {
        CreateHoldingRequestDto request = new CreateHoldingRequestDto();
        request.setProductId(productId);
        request.setBalance(new BigDecimal(balance));
        return request;
    }

    @Test
    void addHolding_savesAndReturnsDto_whenInvestorAndProductExist() {
        CreateHoldingRequestDto request = createHoldingRequest(2L, "5000.00");

        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> {
            Holding saved = inv.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        HoldingDto result = portfolioService.addHolding(1L, request);

        assertEquals(7L, result.getHoldingId());
        assertEquals(product.getId(), result.getProductId());
        assertEquals(new BigDecimal("5000.00"), result.getBalance());
        assertEquals(new BigDecimal("4500.00"), result.getMaxWithdrawable());
    }

    @Test
    void addHolding_throwsNotFound_whenInvestorDoesNotExist() {
        CreateHoldingRequestDto request = createHoldingRequest(2L, "5000.00");
        when(investorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> portfolioService.addHolding(99L, request));
        verifyNoInteractions(productRepository);
        verify(holdingRepository, never()).save(any());
    }

    @Test
    void addHolding_throwsNotFound_whenProductDoesNotExist() {
        CreateHoldingRequestDto request = createHoldingRequest(404L, "5000.00");
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> portfolioService.addHolding(1L, request));
        verify(holdingRepository, never()).save(any());
    }

    @Test
    void addHolding_allowsZeroOpeningBalance() {
        CreateHoldingRequestDto request = createHoldingRequest(2L, "0.00");
        when(investorRepository.findById(1L)).thenReturn(Optional.of(investor));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> {
            Holding saved = inv.getArgument(0);
            saved.setId(8L);
            return saved;
        });

        HoldingDto result = portfolioService.addHolding(1L, request);

        assertEquals(new BigDecimal("0.00"), result.getBalance());
        assertEquals(new BigDecimal("0.00"), result.getMaxWithdrawable());
    }

    // =====================================================================
    // getAllProducts()
    // =====================================================================

    @Test
    void getAllProducts_returnsMappedDtos() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductDto> result = portfolioService.getAllProducts();

        assertEquals(1, result.size());
        assertEquals(product.getId(), result.get(0).getId());
        assertEquals(product.getName(), result.get(0).getName());
        assertEquals(product.getType(), result.get(0).getType());
    }

    @Test
    void getAllProducts_returnsEmptyList_whenNoProductsExist() {
        when(productRepository.findAll()).thenReturn(List.of());

        assertTrue(portfolioService.getAllProducts().isEmpty());
    }
}
