package com.kavin.xeno.crm.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kavin.xeno.crm.entity.Campaign;
import com.kavin.xeno.crm.entity.Communication;
import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.repository.CampaignRepository;
import com.kavin.xeno.crm.repository.CommunicationRepository;
import com.kavin.xeno.crm.repository.CustomerRepository;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CustomerRepository customerRepository;
    private final CommunicationRepository communicationRepository;
    private final RestTemplate restTemplate;

    public CampaignService(
            CampaignRepository campaignRepository,
            CustomerRepository customerRepository,
            CommunicationRepository communicationRepository,
            RestTemplate restTemplate) {

        this.campaignRepository = campaignRepository;
        this.customerRepository = customerRepository;
        this.communicationRepository = communicationRepository;
        this.restTemplate = restTemplate;
    }

    public Campaign saveCampaign(Campaign campaign) {

        if (campaign.getCreatedAt() == null) {
            campaign.setCreatedAt(LocalDateTime.now());
        }

        if (campaign.getStatus() == null) {
            campaign.setStatus("DRAFT");
        }

        return campaignRepository.save(campaign);
    }

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    public String launchCampaign(Long campaignId) {

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        List<Customer> customers = customerRepository.findAll();

        for (Customer customer : customers) {

            Communication communication = new Communication();
            communication.setCampaignId(campaignId);
            communication.setCustomerId(customer.getId());
            communication.setStatus("SENT");
            communicationRepository.save(communication);

            Map<String, Object> request = new HashMap<>();
            request.put("campaignId", campaignId);
            request.put("customerId", customer.getId());
            request.put("message", campaign.getMessage());
            request.put("channel", campaign.getChannel());

            restTemplate.postForObject(
                    "http://localhost:8081/send",
                    request,
                    String.class
            );
        }

        campaign.setStatus("RUNNING");
        campaignRepository.save(campaign);

        return "Campaign launched successfully";
    }
}