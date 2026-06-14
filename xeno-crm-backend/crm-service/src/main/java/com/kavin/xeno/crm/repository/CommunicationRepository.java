package com.kavin.xeno.crm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kavin.xeno.crm.entity.Communication;

public interface CommunicationRepository extends JpaRepository<Communication, Long> {

    Optional<Communication> findByCampaignIdAndCustomerId(Long campaignId, Long customerId);
}