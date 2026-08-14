package com.enviro.assessment.junior.sive.bootstrap;

import com.enviro.assessment.junior.sive.entity.*;
import com.enviro.assessment.junior.sive.repository.HoldingRepository;
import com.enviro.assessment.junior.sive.repository.InvestorRepository;
import com.enviro.assessment.junior.sive.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataLoader implements CommandLineRunner {

    private final InvestorRepository investorRepository;
    private final ProductRepository productRepository;
    private final HoldingRepository holdingRepository;

    public DataLoader(InvestorRepository investorRepository, ProductRepository productRepository,
                       HoldingRepository holdingRepository) {
        this.investorRepository = investorRepository;
        this.productRepository = productRepository;
        this.holdingRepository = holdingRepository;
    }

    @Override
    public void run(String... args) {
        Product retirementAnnuity = productRepository.save(new Product("Enviro365 Retirement Annuity", ProductType.RETIREMENT_ANNUITY));
        Product savingsPlan = productRepository.save(new Product("Enviro365 Flexible Savings Plan", ProductType.SAVINGS_PLAN));
        Product discretionary = productRepository.save(new Product("Enviro365 Discretionary Investment", ProductType.DISCRETIONARY_INVESTMENT));

        Investor thandiwe = investorRepository.save(new Investor(
                "Thandiwe Nkosi", "thandiwe.nkosi@example.com", LocalDate.now().minusYears(70).minusDays(20)));
        Investor sipho = investorRepository.save(new Investor(
                "Sipho Dlamini", "sipho.dlamini@example.com", LocalDate.now().minusYears(42).minusDays(10)));
        Investor lindiwe = investorRepository.save(new Investor(
                "Lindiwe Khumalo", "lindiwe.khumalo@example.com", LocalDate.now().minusYears(66).minusDays(5)));

        holdingRepository.save(new Holding(thandiwe, retirementAnnuity, new BigDecimal("450000.00")));
        holdingRepository.save(new Holding(thandiwe, savingsPlan, new BigDecimal("85000.00")));

        holdingRepository.save(new Holding(sipho, retirementAnnuity, new BigDecimal("210000.00")));
        holdingRepository.save(new Holding(sipho, discretionary, new BigDecimal("60000.00")));

        holdingRepository.save(new Holding(lindiwe, retirementAnnuity, new BigDecimal("320000.00")));
        holdingRepository.save(new Holding(lindiwe, savingsPlan, new BigDecimal("15000.00")));
        holdingRepository.save(new Holding(lindiwe, discretionary, new BigDecimal("40000.00")));
    }
}
