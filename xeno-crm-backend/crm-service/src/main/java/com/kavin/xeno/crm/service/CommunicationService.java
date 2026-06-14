package com.kavin.xeno.crm.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.kavin.xeno.crm.entity.Communication;
import com.kavin.xeno.crm.repository.CommunicationRepository;

@Service
public class CommunicationService {

    private final CommunicationRepository communicationRepository;

    public CommunicationService(CommunicationRepository communicationRepository) {
        this.communicationRepository = communicationRepository;
    }

    public Communication saveCommunication(Communication communication) {
        return communicationRepository.save(communication);
    }

    public List<Communication> getAllCommunications() {
        return communicationRepository.findAll();
    }

    public Optional<Communication> findByCampaignIdAndCustomerId(Long campaignId, Long customerId) {
        return communicationRepository.findByCampaignIdAndCustomerId(campaignId, customerId);
    }
}
