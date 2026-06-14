package com.kavin.xeno.crm.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kavin.xeno.crm.service.AiCampaignService;

@RestController
@RequestMapping("/ai")
@CrossOrigin
public class AiController {

    private final AiCampaignService aiCampaignService;

    public AiController(AiCampaignService aiCampaignService) {
        this.aiCampaignService = aiCampaignService;
    }

    @PostMapping("/generate-campaign")
    public Map<String, String> generateCampaign(
            @RequestBody Map<String,String> request) {

        return aiCampaignService.generateCampaign(
                request.get("goal")
        );
    }
}