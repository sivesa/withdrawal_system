package com.enviro.assessment.junior.sive.service;

import com.enviro.assessment.junior.sive.dto.*;
import com.enviro.assessment.junior.sive.entity.Holding;
import com.enviro.assessment.junior.sive.entity.Investor;
import com.enviro.assessment.junior.sive.entity.Product;
import com.enviro.assessment.junior.sive.exception.DuplicateResourceException;
import com.enviro.assessment.junior.sive.exception.ResourceNotFoundException;
import com.enviro.assessment.junior.sive.repository.HoldingRepository;
import com.enviro.assessment.junior.sive.repository.InvestorRepository;
import com.enviro.assessment.junior.sive.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private static final BigDecimal NINETY_PERCENT = new BigDecimal("0.90");

    private final InvestorRepository investorRepository;
    private final HoldingRepository holdingRepository;
    private final ProductRepository productRepository;

    public PortfolioService(InvestorRepository investorRepository, HoldingRepository holdingRepository,
                            ProductRepository productRepository) {
        this.investorRepository = investorRepository;
        this.holdingRepository = holdingRepository;
        this.productRepository = productRepository;
    }

    /**
     * @Transactional(readOnly = true) matters here: with spring.jpa.open-in-view=false
     * (see application.properties), the Hibernate session normally closes the moment
     * the repository call returns. Product is a LAZY @ManyToOne on Holding, so without
     * an active transaction spanning both the query AND the toHoldingDto() mapping
     * below, holding.getProduct().getName() throws
     * "could not initialize proxy - no Session". Wrapping the whole method keeps one
     * session open across the query and the DTO conversion.
     */
    @Transactional(readOnly = true)
    public List<InvestorDto> getAllInvestors() {
        return investorRepository.findAll().stream()
                .map(this::toInvestorDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PortfolioResponseDto getPortfolio(Long investorId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found with id: " + investorId));

        List<HoldingDto> holdings = holdingRepository.findByInvestorId(investorId).stream()
                .map(this::toHoldingDto)
                .collect(Collectors.toList());

        return new PortfolioResponseDto(toInvestorDto(investor), holdings);
    }

    /** Used by the new admin "Add Investor" page. */
    @Transactional
    public InvestorDto createInvestor(CreateInvestorRequestDto request) {
        if (investorRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("An investor with email '" + request.getEmail() + "' already exists.");
        }
        Investor investor = new Investor(request.getFullName(), request.getEmail(), request.getDateOfBirth());
        return toInvestorDto(investorRepository.save(investor));
    }

    /** Used by the admin page to optionally give a newly created investor an opening holding. */
    @Transactional
    public HoldingDto addHolding(Long investorId, CreateHoldingRequestDto request) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found with id: " + investorId));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        Holding holding = new Holding(investor, product, request.getBalance());
        return toHoldingDto(holdingRepository.save(holding));
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(p -> new ProductDto(p.getId(), p.getName(), p.getType()))
                .collect(Collectors.toList());
    }

    public InvestorDto toInvestorDto(Investor investor) {
        return new InvestorDto(investor.getId(), investor.getFullName(), investor.getEmail(),
                investor.getDateOfBirth(), investor.getAge());
    }

    public HoldingDto toHoldingDto(Holding holding) {
        BigDecimal maxWithdrawable = holding.getBalance().multiply(NINETY_PERCENT).setScale(2, RoundingMode.DOWN);
        return new HoldingDto(
                holding.getId(),
                holding.getProduct().getId(),
                holding.getProduct().getName(),
                holding.getProduct().getType(),
                holding.getBalance(),
                maxWithdrawable);
    }
}