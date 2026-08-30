package com.kavin.xeno.crm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kavin.xeno.crm.entity.Campaign;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByUserId(Long userId);
    Optional<Campaign> findByIdAndUserId(Long id, Long userId);
}