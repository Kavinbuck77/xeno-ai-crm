package com.kavin.xeno.crm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kavin.xeno.crm.entity.Campaign;
import com.kavin.xeno.crm.service.CampaignService;

@RestController
@RequestMapping("/campaigns")
@CrossOrigin
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public Campaign createCampaign(@RequestBody Campaign campaign) {
        return campaignService.saveCampaign(campaign);
    }

    @GetMapping
    public List<Campaign> getAllCampaigns() {
        return campaignService.getAllCampaigns();
    }

    @GetMapping("/{id}")
    public Campaign getCampaignById(@PathVariable Long id) {
        return campaignService.getCampaignById(id);
    }

    @PostMapping("/{id}/launch")
    public ResponseEntity<Map<String, Object>> launchCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.launchCampaign(id));
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<Map<String, Object>> getCampaignAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaignAnalytics(id));
    }

    @GetMapping("/{id}/recipients")
    public ResponseEntity<List<Map<String, Object>>> getCampaignRecipients(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaignRecipients(id));
    }
}