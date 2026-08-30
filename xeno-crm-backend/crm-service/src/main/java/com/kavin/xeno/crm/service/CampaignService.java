package com.kavin.xeno.crm.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kavin.xeno.crm.entity.Campaign;
import com.kavin.xeno.crm.entity.Communication;
import com.kavin.xeno.crm.entity.CommunicationStatus;
import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.repository.CampaignRepository;
import com.kavin.xeno.crm.repository.CommunicationRepository;
import com.kavin.xeno.crm.repository.CustomerRepository;
import com.kavin.xeno.crm.security.SecurityUtils;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CommunicationRepository communicationRepository;
    private final SegmentationService segmentationService;
    private final CampaignDispatcher campaignDispatcher;
    private final CustomerRepository customerRepository;

    public CampaignService(
            CampaignRepository campaignRepository,
            CommunicationRepository communicationRepository,
            SegmentationService segmentationService,
            CampaignDispatcher campaignDispatcher,
            CustomerRepository customerRepository) {

        this.campaignRepository = campaignRepository;
        this.communicationRepository = communicationRepository;
        this.segmentationService = segmentationService;
        this.campaignDispatcher = campaignDispatcher;
        this.customerRepository = customerRepository;
    }

    public Campaign saveCampaign(Campaign campaign) {
        campaign.setUserId(SecurityUtils.getCurrentUserId());

        if (campaign.getCreatedAt() == null) {
            campaign.setCreatedAt(LocalDateTime.now());
        }

        if (campaign.getStatus() == null) {
            campaign.setStatus("DRAFT");
        }

        return campaignRepository.save(campaign);
    }

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findByUserId(SecurityUtils.getCurrentUserId());
    }

    public Campaign getCampaignById(Long id) {
        return campaignRepository.findByIdAndUserId(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Campaign not found or access denied"));
    }

    @Transactional
    public Map<String, Object> launchCampaign(Long campaignId) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 1. Fetch and validate campaign ownership
        Campaign campaign = campaignRepository.findByIdAndUserId(campaignId, userId)
                .orElseThrow(() -> new RuntimeException("Campaign not found or access denied"));

        if (!"DRAFT".equals(campaign.getStatus())) {
            throw new IllegalArgumentException("Only campaigns in DRAFT status can be launched");
        }

        // 2. Idempotency check: ensure no communications already exist for this campaign
        long existingCount = communicationRepository.countByCampaignId(campaignId);
        if (existingCount > 0) {
            throw new IllegalArgumentException("Campaign has already been dispatched. " + existingCount + " communications exist.");
        }

        // 3. Resolve recipients based on segment criteria
        List<Customer> recipients = segmentationService.resolveRecipients(userId, campaign.getSegmentType(), campaign.getSegmentCriteriaJson());

        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("No customers match this audience criteria.");
        }

        // 4. Create QUEUED communication records
        List<Communication> communications = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Customer customer : recipients) {
            Communication communication = new Communication();
            communication.setCampaignId(campaignId);
            communication.setCustomerId(customer.getId());
            communication.setChannel(campaign.getChannel());
            communication.setStatus(CommunicationStatus.QUEUED);
            communication.setCreatedAt(now);
            communications.add(communication);
        }
        
        List<Communication> savedCommunications = communicationRepository.saveAll(communications);

        // 5. Update campaign status to RUNNING
        campaign.setStatus("RUNNING");
        campaignRepository.save(campaign);

        // 6. Extract IDs and trigger asynchronous dispatch (pass IDs, not managed entities)
        List<Long> communicationIds = savedCommunications.stream()
                .map(Communication::getId)
                .collect(java.util.stream.Collectors.toList());
        campaignDispatcher.dispatch(campaignId, communicationIds);

        // 7. Return response immediately
        Map<String, Object> response = new HashMap<>();
        response.put("status", "QUEUED");
        response.put("recipientCount", savedCommunications.size());
        return response;
    }

    public Map<String, Object> getCampaignAnalytics(Long campaignId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Campaign campaign = campaignRepository.findByIdAndUserId(campaignId, userId)
                .orElseThrow(() -> new RuntimeException("Campaign not found or access denied"));

        long total = communicationRepository.countByCampaignId(campaignId);
        long queued = communicationRepository.countByCampaignIdAndStatus(campaignId, CommunicationStatus.QUEUED);
        long sent = communicationRepository.countByCampaignIdAndStatus(campaignId, CommunicationStatus.SENT);
        long delivered = communicationRepository.countByCampaignIdAndStatus(campaignId, CommunicationStatus.DELIVERED);
        long failed = communicationRepository.countByCampaignIdAndStatus(campaignId, CommunicationStatus.FAILED);

        double deliveryRate = total > 0 ? ((double) delivered / total) * 100 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecipients", total);
        stats.put("queued", queued);
        stats.put("sent", sent);
        stats.put("delivered", delivered);
        stats.put("failed", failed);
        stats.put("deliveryRate", Math.round(deliveryRate * 10.0) / 10.0);

        return stats;
    }

    public List<Map<String, Object>> getCampaignRecipients(Long campaignId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Campaign campaign = campaignRepository.findByIdAndUserId(campaignId, userId)
                .orElseThrow(() -> new RuntimeException("Campaign not found or access denied"));

        List<Communication> communications = communicationRepository.findByCampaignId(campaignId);
        List<Map<String, Object>> recipientsList = new java.util.ArrayList<>();

        for (Communication comm : communications) {
            Map<String, Object> map = new HashMap<>();
            map.put("communicationId", comm.getId());
            map.put("customerId", comm.getCustomerId());
            map.put("status", comm.getStatus().toString());
            map.put("createdAt", comm.getCreatedAt());
            map.put("deliveredAt", comm.getDeliveredAt());
            map.put("errorMessage", comm.getErrorMessage());

            customerRepository.findById(comm.getCustomerId()).ifPresent(c -> {
                map.put("customerName", c.getName());
                map.put("customerEmail", c.getEmail());
                map.put("customerPhone", c.getPhone());
            });

            recipientsList.add(map);
        }

        return recipientsList;
    }
}