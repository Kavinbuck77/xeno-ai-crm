package com.kavin.xeno.crm.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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

    private final CommunicationService communicationService;

    public ReceiptController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @PostMapping
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
                    communication.setStatus(status);
                    
                    if (status == CommunicationStatus.DELIVERED || status == CommunicationStatus.FAILED) {
                        communication.setDeliveredAt(LocalDateTime.now());
                    }
                    if (status == CommunicationStatus.FAILED) {
                        communication.setErrorMessage(errorMessage);
                    }
                    
                    communicationService.saveCommunication(communication);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid communication status received in callback: " + statusStr);
                }
            } else {
                System.err.println("No existing communication record found for campaignId " + campaignId + " and customerId " + customerId);
            }
        }

        System.out.println("=================================");
        System.out.println("RECEIPT RECEIVED");
        System.out.println(payload);
        System.out.println("=================================");

        return "Receipt Processed";
    }
}