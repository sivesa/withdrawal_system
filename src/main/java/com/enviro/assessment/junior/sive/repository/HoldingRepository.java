package com.enviro.assessment.junior.sive.repository;

import com.enviro.assessment.junior.sive.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByInvestorId(Long investorId);
}
