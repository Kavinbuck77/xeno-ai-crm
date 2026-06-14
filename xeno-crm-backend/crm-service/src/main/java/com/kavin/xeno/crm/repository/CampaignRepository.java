package com.kavin.xeno.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kavin.xeno.crm.entity.Campaign;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
}