package com.kavin.xeno.channel.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kavin.xeno.channel.dto.SendRequest;
import com.kavin.xeno.channel.service.ChannelDeliveryService;

@RestController
@RequestMapping("/send")
@CrossOrigin
public class ChannelController {

    private final ChannelDeliveryService deliveryService;

    public ChannelController(ChannelDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    public String sendMessage(@RequestBody SendRequest request) {
        if (request.getCampaignId() == null || request.getCustomerId() == null || request.getMessage() == null) {
            throw new IllegalArgumentException("Invalid send request payload");
        }

        deliveryService.simulateDelivery(request);

        return "Message Accepted";
    }
}