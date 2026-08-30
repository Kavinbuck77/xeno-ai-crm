package com.kavin.xeno.crm.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kavin.xeno.crm.entity.Campaign;
import com.kavin.xeno.crm.entity.Communication;
import com.kavin.xeno.crm.entity.CommunicationStatus;
import com.kavin.xeno.crm.repository.CampaignRepository;
import com.kavin.xeno.crm.repository.CommunicationRepository;

@Service
public class CampaignDispatcher {

    private final CommunicationRepository communicationRepository;
    private final CampaignRepository campaignRepository;
    private final RestTemplate restTemplate;

    @Value("${channel.service.url:http://localhost:8081}")
    private String channelServiceUrl;

    public CampaignDispatcher(CommunicationRepository communicationRepository,
                              CampaignRepository campaignRepository,
                              RestTemplate restTemplate) {
        this.communicationRepository = communicationRepository;
        this.campaignRepository = campaignRepository;
        this.restTemplate = restTemplate;
    }

    @Async
    public void dispatch(Campaign campaign, List<Communication> communications) {
        System.out.println("Starting asynchronous campaign dispatch for campaign: " + campaign.getId());

        for (Communication comm : communications) {
            try {
                comm.setStatus(CommunicationStatus.SENT);
                communicationRepository.save(comm);

                Map<String, Object> request = new HashMap<>();
                request.put("campaignId", campaign.getId());
                request.put("customerId", comm.getCustomerId());
                request.put("message", campaign.getMessage());
                request.put("channel", campaign.getChannel());

                restTemplate.postForObject(
                        channelServiceUrl + "/send",
                        request,
                        String.class
                );

            } catch (Exception e) {
                System.err.println("Failed to dispatch communication ID " + comm.getId() + ": " + e.getMessage());
                comm.setStatus(CommunicationStatus.FAILED);
                comm.setErrorMessage("Dispatch failed: " + e.getMessage());
                communicationRepository.save(comm);
            }
        }

        campaign.setStatus("SENT");
        campaignRepository.save(campaign);
        System.out.println("Asynchronous campaign dispatch finished for campaign: " + campaign.getId());
    }
}
