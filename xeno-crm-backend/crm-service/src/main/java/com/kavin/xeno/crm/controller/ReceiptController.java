package com.kavin.xeno.crm.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kavin.xeno.crm.entity.Communication;
import com.kavin.xeno.crm.entity.CommunicationStatus;
import com.kavin.xeno.crm.service.CommunicationService;

@RestController
@RequestMapping("/receipts")
@CrossOrigin
public class ReceiptController {

    private static final Logger log = LoggerFactory.getLogger(ReceiptController.class);

    private final CommunicationService communicationService;

    public ReceiptController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @PostMapping
    @Transactional
    public String receiveReceipt(@RequestBody Map<String, Object> payload) {
        Long campaignId = payload.containsKey("campaignId") && payload.get("campaignId") != null
                ? ((Number) payload.get("campaignId")).longValue()
                : null;
        Long customerId = payload.containsKey("customerId") && payload.get("customerId") != null
                ? ((Number) payload.get("customerId")).longValue()
                : null;
        String statusStr = payload.containsKey("status") && payload.get("status") != null
                ? payload.get("status").toString().toUpperCase()
                : "DELIVERED";
        String errorMessage = payload.containsKey("errorMessage") && payload.get("errorMessage") != null
                ? payload.get("errorMessage").toString()
                : null;

        if (campaignId != null && customerId != null) {
            Optional<Communication> existing = communicationService
                    .findByCampaignIdAndCustomerId(campaignId, customerId);

            if (existing.isPresent()) {
                Communication communication = existing.get();
                try {
                    CommunicationStatus status = CommunicationStatus.valueOf(statusStr);

                    // Only update if the status transition is valid (don't regress)
                    if (isValidTransition(communication.getStatus(), status)) {
                        communication.setStatus(status);

                        if (status == CommunicationStatus.DELIVERED || status == CommunicationStatus.FAILED) {
                            communication.setDeliveredAt(LocalDateTime.now());
                        }
                        if (status == CommunicationStatus.FAILED) {
                            communication.setErrorMessage(errorMessage);
                        }

                        communicationService.saveCommunication(communication);
                        log.info("Receipt processed: campaign={}, customer={}, status={}", campaignId, customerId, status);
                    } else {
                        log.warn("Ignoring invalid status transition from {} to {} for campaign={}, customer={}",
                                communication.getStatus(), status, campaignId, customerId);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid communication status received in callback: {}", statusStr);
                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                    // Another transaction updated this communication concurrently.
                    // This is expected during high-throughput dispatch — log and accept.
                    log.warn("Optimistic lock conflict for campaign={}, customer={}. " +
                            "Another transaction updated this communication. Receipt status: {}", campaignId, customerId, statusStr);
                }
            } else {
                log.warn("No existing communication record found for campaignId {} and customerId {}", campaignId, customerId);
            }
        }

        log.debug("Receipt received: {}", payload);

        return "Receipt Processed";
    }

    /**
     * Validate that the status transition is forward-progressing.
     * Prevents a late-arriving callback from overwriting a final status.
     */
    private boolean isValidTransition(CommunicationStatus current, CommunicationStatus target) {
        if (current == null) return true;

        // Once DELIVERED or FAILED, don't allow further transitions
        if (current == CommunicationStatus.DELIVERED || current == CommunicationStatus.FAILED) {
            return false;
        }

        // QUEUED can go to SENT, DELIVERED, or FAILED
        // SENT can go to DELIVERED or FAILED
        return true;
    }
}