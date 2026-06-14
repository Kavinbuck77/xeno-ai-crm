package com.kavin.xeno.channel.controller;

import com.kavin.xeno.channel.dto.SendRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/send")
@CrossOrigin
public class ChannelController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public String sendMessage(@RequestBody SendRequest request)
            throws InterruptedException {

        Thread.sleep(2000);

        Map<String, Object> callback = new HashMap<>();
        callback.put("campaignId", request.getCampaignId());
        callback.put("customerId", request.getCustomerId());
        callback.put("status", "DELIVERED");

        restTemplate.postForObject(
                "http://localhost:8080/receipts",
                callback,
                String.class
        );

        return "Message Accepted";
    }
}