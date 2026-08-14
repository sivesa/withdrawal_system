package com.enviro.assessment.junior.sive.repository;

import com.enviro.assessment.junior.sive.entity.Investor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestorRepository extends JpaRepository<Investor, Long> {
    boolean existsByEmailIgnoreCase(String email);
}
