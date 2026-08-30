package com.kavin.xeno.channel.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kavin.xeno.channel.dto.SendRequest;

@Service
public class ChannelDeliveryService {

    private final RestTemplate restTemplate;
    private final Random random = new Random();

    @Value("${crm.service.url:http://localhost:8080}")
    private String crmServiceUrl;

    @Value("${channel.simulated.latency.ms:2000}")
    private long simulatedLatencyMs;

    public ChannelDeliveryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async
    public void simulateDelivery(SendRequest request) {
        try {
            Thread.sleep(simulatedLatencyMs);

            String status = "DELIVERED";
            String errorMessage = null;
            if (random.nextDouble() < 0.05) {
                status = "FAILED";
                String[] errors = {
                    "Recipient mailbox full",
                    "Invalid phone number or email address",
                    "Carrier network timeout",
                    "Device unreachable"
                };
                errorMessage = errors[random.nextInt(errors.length)];
            }

            Map<String, Object> callback = new HashMap<>();
            callback.put("campaignId", request.getCampaignId());
            callback.put("customerId", request.getCustomerId());
            callback.put("status", status);
            if (errorMessage != null) {
                callback.put("errorMessage", errorMessage);
            }

            System.out.println("ChannelService: Dispatching callback receipt to " + crmServiceUrl + "/receipts - Status: " + status);

            restTemplate.postForObject(
                    crmServiceUrl + "/receipts",
                    callback,
                    String.class
            );

        } catch (InterruptedException e) {
            System.err.println("Delivery simulation interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Failed to deliver callback to CRM service: " + e.getMessage());
        }
    }
}
