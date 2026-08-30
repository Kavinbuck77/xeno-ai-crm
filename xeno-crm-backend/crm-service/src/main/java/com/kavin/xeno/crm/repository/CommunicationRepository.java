package com.kavin.xeno.crm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kavin.xeno.crm.entity.Communication;
import com.kavin.xeno.crm.entity.CommunicationStatus;

@Repository
public interface CommunicationRepository extends JpaRepository<Communication, Long> {

    Optional<Communication> findByCampaignIdAndCustomerId(Long campaignId, Long customerId);

    List<Communication> findByCampaignId(Long campaignId);

    long countByCampaignId(Long campaignId);

    long countByCampaignIdAndStatus(Long campaignId, CommunicationStatus status);

    @Query("SELECT COUNT(c) FROM Communication c JOIN Campaign cam ON c.campaignId = cam.id WHERE cam.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM Communication c JOIN Campaign cam ON c.campaignId = cam.id WHERE cam.userId = :userId AND c.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CommunicationStatus status);
}