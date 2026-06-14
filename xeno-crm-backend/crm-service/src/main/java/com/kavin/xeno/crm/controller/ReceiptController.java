package com.kavin.xeno.crm.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kavin.xeno.crm.entity.Communication;
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

        Long campaignId = payload.containsKey("campaignId")
                ? ((Number) payload.get("campaignId")).longValue()
                : null;
        Long customerId = payload.containsKey("customerId")
                ? ((Number) payload.get("customerId")).longValue()
                : null;
        String status = payload.containsKey("status")
                ? payload.get("status").toString()
                : "DELIVERED";

        if (campaignId != null && customerId != null) {
            Optional<Communication> existing = communicationService
                    .findByCampaignIdAndCustomerId(campaignId, customerId);

            Communication communication = existing.orElseGet(Communication::new);
            communication.setCampaignId(campaignId);
            communication.setCustomerId(customerId);
            communication.setStatus(status);
            communicationService.saveCommunication(communication);
        }

        System.out.println("=================================");
        System.out.println("RECEIPT RECEIVED");
        System.out.println(payload);
        System.out.println("=================================");

        return "Receipt Processed";
    }
}