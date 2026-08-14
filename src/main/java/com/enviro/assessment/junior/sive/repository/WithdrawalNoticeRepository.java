package com.enviro.assessment.junior.sive.repository;

import com.enviro.assessment.junior.sive.entity.WithdrawalNotice;
import com.enviro.assessment.junior.sive.entity.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalNoticeRepository extends JpaRepository<WithdrawalNotice, Long> {
    List<WithdrawalNotice> findByInvestorIdOrderByCreatedAtDesc(Long investorId);
    List<WithdrawalNotice> findByInvestorIdAndStatusOrderByCreatedAtDesc(Long investorId, WithdrawalStatus status);
    List<WithdrawalNotice> findAllByOrderByCreatedAtDesc();
    List<WithdrawalNotice> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status);
}
