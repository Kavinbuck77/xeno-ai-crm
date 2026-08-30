package com.kavin.xeno.crm.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.kavin.xeno.crm.entity.Campaign;
import com.kavin.xeno.crm.entity.Communication;
import com.kavin.xeno.crm.entity.CommunicationStatus;
import com.kavin.xeno.crm.repository.CampaignRepository;
import com.kavin.xeno.crm.repository.CommunicationRepository;

@Service
public class CampaignDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CampaignDispatcher.class);

    private final CommunicationRepository communicationRepository;
    private final CampaignRepository campaignRepository;
    private final RestTemplate restTemplate;

    private static final int MAX_RETRIES = 2;
    private static final long DISPATCH_DELAY_MS = 200;
    private static final long RETRY_BASE_DELAY_MS = 1000;

    @Value("${channel.service.url:http://localhost:8081}")
    private String channelServiceUrl;

    public CampaignDispatcher(CommunicationRepository communicationRepository,
                              CampaignRepository campaignRepository,
                              RestTemplate restTemplate) {
        this.communicationRepository = communicationRepository;
        this.campaignRepository = campaignRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Asynchronously dispatch communications for a campaign.
     * Accepts communication IDs (not managed entities) to avoid stale entity issues
     * across the async transaction boundary.
     */
    @Async
    public void dispatch(Long campaignId, List<Long> communicationIds) {
        log.info("Starting asynchronous campaign dispatch for campaign: {}, communications: {}",
                campaignId, communicationIds.size());

        for (Long commId : communicationIds) {
            try {
                dispatchSingleCommunication(campaignId, commId);

                // Small delay between dispatches to avoid overwhelming the channel service
                Thread.sleep(DISPATCH_DELAY_MS);
            } catch (InterruptedException e) {
                log.warn("Campaign dispatch interrupted for campaign: {}", campaignId);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected error dispatching communication ID {}: {}", commId, e.getMessage());
            }
        }

        // Update campaign status to SENT after all dispatches complete
        try {
            campaignRepository.findById(campaignId).ifPresent(campaign -> {
                campaign.setStatus("SENT");
                campaignRepository.save(campaign);
                log.info("Campaign {} status updated to SENT", campaignId);
            });
        } catch (Exception e) {
            log.error("Failed to update campaign {} status to SENT: {}", campaignId, e.getMessage());
        }

        log.info("Asynchronous campaign dispatch finished for campaign: {}", campaignId);
    }

    /**
     * Dispatch a single communication with proper transaction handling.
     * Reloads the entity from the database to ensure fresh state.
     */
    private void dispatchSingleCommunication(Long campaignId, Long commId) {
        // 1. Reload the communication from DB (fresh state, not stale)
        Optional<Communication> optComm = communicationRepository.findById(commId);
        if (optComm.isEmpty()) {
            log.warn("Communication {} not found, skipping", commId);
            return;
        }

        Communication comm = optComm.get();

        // 2. Idempotency check: only dispatch if still QUEUED
        if (comm.getStatus() != CommunicationStatus.QUEUED) {
            log.info("Communication {} is already in status {}, skipping dispatch", commId, comm.getStatus());
            return;
        }

        // 3. Mark as SENT (in-progress)
        comm.setStatus(CommunicationStatus.SENT);
        communicationRepository.save(comm);

        // 4. Call channel service with retry logic for 429
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            log.error("Campaign {} not found during dispatch", campaignId);
            markFailed(commId, "Campaign not found during dispatch");
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("campaignId", campaignId);
        request.put("customerId", comm.getCustomerId());
        request.put("message", campaign.getMessage());
        request.put("channel", campaign.getChannel());

        boolean sent = false;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                restTemplate.postForObject(
                        channelServiceUrl + "/send",
                        request,
                        String.class
                );
                sent = true;
                break;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429) {
                    log.warn("429 Too Many Requests for communication {}. Attempt {}/{}",
                            commId, attempt + 1, MAX_RETRIES + 1);
                    if (attempt < MAX_RETRIES) {
                        try {
                            long delay = RETRY_BASE_DELAY_MS * (long) Math.pow(2, attempt);
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            markFailed(commId, "Dispatch interrupted during retry");
                            return;
                        }
                    } else {
                        markFailed(commId, "Channel service rate limited (429) after " + (MAX_RETRIES + 1) + " attempts");
                        return;
                    }
                } else {
                    markFailed(commId, "Channel service HTTP error: " + e.getStatusCode().value());
                    return;
                }
            } catch (Exception e) {
                markFailed(commId, "Dispatch failed: " + e.getMessage());
                return;
            }
        }

        if (!sent) {
            markFailed(commId, "Failed to send after all retry attempts");
        }
    }

    /**
     * Mark a communication as FAILED with an error message.
     * Reloads from DB to avoid stale entity issues.
     */
    private void markFailed(Long commId, String errorMessage) {
        try {
            communicationRepository.findById(commId).ifPresent(comm -> {
                comm.setStatus(CommunicationStatus.FAILED);
                comm.setErrorMessage(errorMessage);
                communicationRepository.save(comm);
                log.warn("Communication {} marked as FAILED: {}", commId, errorMessage);
            });
        } catch (Exception e) {
            log.error("Failed to mark communication {} as FAILED: {}", commId, e.getMessage());
        }
    }
}
