package com.kavin.xeno.crm.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kavin.xeno.crm.entity.CommunicationStatus;
import com.kavin.xeno.crm.repository.CampaignRepository;
import com.kavin.xeno.crm.repository.CommunicationRepository;
import com.kavin.xeno.crm.repository.CustomerRepository;
import com.kavin.xeno.crm.security.SecurityUtils;

@Service
public class AnalyticsService {

    private final CustomerRepository customerRepository;
    private final CampaignRepository campaignRepository;
    private final CommunicationRepository communicationRepository;

    public AnalyticsService(CustomerRepository customerRepository,
                            CampaignRepository campaignRepository,
                            CommunicationRepository communicationRepository) {
        this.customerRepository = customerRepository;
        this.campaignRepository = campaignRepository;
        this.communicationRepository = communicationRepository;
    }

    public Map<String, Object> getSummary() {
        Long userId = SecurityUtils.getCurrentUserId();

        long totalCustomers = customerRepository.findByUserId(userId).size();
        long totalCampaigns = campaignRepository.findByUserId(userId).size();
        long totalSent = communicationRepository.countByUserId(userId);
        long delivered = communicationRepository.countByUserIdAndStatus(userId, CommunicationStatus.DELIVERED);
        long failed = communicationRepository.countByUserIdAndStatus(userId, CommunicationStatus.FAILED);
        long queued = communicationRepository.countByUserIdAndStatus(userId, CommunicationStatus.QUEUED);
        long sentOnly = communicationRepository.countByUserIdAndStatus(userId, CommunicationStatus.SENT);

        double deliveryRate = totalSent > 0 ? ((double) delivered / totalSent) * 100 : 0.0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCustomers", totalCustomers);
        summary.put("totalCampaigns", totalCampaigns);
        summary.put("totalCommunications", totalSent);
        summary.put("deliveredCount", delivered);
        summary.put("failedCount", failed);
        summary.put("queuedCount", queued);
        summary.put("sentOnlyCount", sentOnly);
        summary.put("deliveryRate", Math.round(deliveryRate * 10.0) / 10.0);

        return summary;
    }
}
