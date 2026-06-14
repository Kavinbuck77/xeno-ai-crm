package com.kavin.xeno.channel.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.kavin.xeno.channel.dto.SendRequest;

@RestController
@RequestMapping("/send")
@CrossOrigin
public class ChannelController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${crm.service.url}")
    private String crmServiceUrl;

    @PostMapping
    public String sendMessage(@RequestBody SendRequest request)
            throws InterruptedException {

        // Simulate message delivery delay
        Thread.sleep(2000);

        Map<String, Object> callback = new HashMap<>();
        callback.put("campaignId", request.getCampaignId());
        callback.put("customerId", request.getCustomerId());
        callback.put("status", "DELIVERED");

        restTemplate.postForObject(
                crmServiceUrl + "/receipts",
                callback,
                String.class
        );

        return "Message Accepted";
    }
}