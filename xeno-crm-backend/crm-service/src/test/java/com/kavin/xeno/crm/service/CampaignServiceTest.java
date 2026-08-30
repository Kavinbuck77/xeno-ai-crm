package com.kavin.xeno.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kavin.xeno.crm.entity.Campaign;
import com.kavin.xeno.crm.entity.Communication;
import com.kavin.xeno.crm.entity.CommunicationStatus;
import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.repository.CampaignRepository;
import com.kavin.xeno.crm.repository.CommunicationRepository;
import com.kavin.xeno.crm.repository.CustomerRepository;
import com.kavin.xeno.crm.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
public class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CommunicationRepository communicationRepository;

    @Mock
    private SegmentationService segmentationService;

    @Mock
    private CampaignDispatcher campaignDispatcher;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CampaignService campaignService;

    private final Long testUserId = 42L;

    @Test
    public void testLaunchCampaign_Success() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            Campaign campaign = new Campaign();
            campaign.setId(1L);
            campaign.setStatus("DRAFT");
            campaign.setSegmentType("ALL_CUSTOMERS");
            campaign.setChannel("EMAIL");
            campaign.setUserId(testUserId);

            Customer customer = new Customer();
            customer.setId(10L);

            Communication savedComm = new Communication();
            savedComm.setId(100L);
            savedComm.setCampaignId(1L);
            savedComm.setCustomerId(10L);
            savedComm.setStatus(CommunicationStatus.QUEUED);

            when(campaignRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(campaign));
            when(communicationRepository.countByCampaignId(1L)).thenReturn(0L);
            when(segmentationService.resolveRecipients(eq(testUserId), any(), any()))
                    .thenReturn(Collections.singletonList(customer));
            when(communicationRepository.saveAll(anyList())).thenReturn(Collections.singletonList(savedComm));

            Map<String, Object> result = campaignService.launchCampaign(1L);

            assertNotNull(result);
            assertEquals("QUEUED", result.get("status"));
            assertEquals(1, result.get("recipientCount"));
            assertEquals("RUNNING", campaign.getStatus());

            verify(campaignDispatcher).dispatch(eq(1L), eq(Collections.singletonList(100L)));
        }
    }

    @Test
    public void testLaunchCampaign_ZeroRecipients_ThrowsException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            Campaign campaign = new Campaign();
            campaign.setId(1L);
            campaign.setStatus("DRAFT");
            campaign.setUserId(testUserId);

            when(campaignRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(campaign));
            when(communicationRepository.countByCampaignId(1L)).thenReturn(0L);
            when(segmentationService.resolveRecipients(eq(testUserId), any(), any()))
                    .thenReturn(Collections.emptyList());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> campaignService.launchCampaign(1L));
            assertEquals("No customers match this audience criteria.", ex.getMessage());
        }
    }

    @Test
    public void testLaunchCampaign_RejectNonDraft() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            Campaign campaign = new Campaign();
            campaign.setId(1L);
            campaign.setStatus("RUNNING");

            when(campaignRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(campaign));

            assertThrows(IllegalArgumentException.class, () -> campaignService.launchCampaign(1L));
        }
    }

    @Test
    public void testLaunchCampaign_DuplicateLaunchProtection() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            Campaign campaign = new Campaign();
            campaign.setId(1L);
            campaign.setStatus("DRAFT");

            when(campaignRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(campaign));
            when(communicationRepository.countByCampaignId(1L)).thenReturn(5L); // Communications already exist!

            assertThrows(IllegalArgumentException.class, () -> campaignService.launchCampaign(1L));
        }
    }

    @Test
    public void testGetCampaignAnalytics() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            Campaign campaign = new Campaign();
            campaign.setId(1L);

            when(campaignRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(campaign));
            when(communicationRepository.countByCampaignId(1L)).thenReturn(10L);
            when(communicationRepository.countByCampaignIdAndStatus(1L, CommunicationStatus.QUEUED)).thenReturn(0L);
            when(communicationRepository.countByCampaignIdAndStatus(1L, CommunicationStatus.SENT)).thenReturn(0L);
            when(communicationRepository.countByCampaignIdAndStatus(1L, CommunicationStatus.DELIVERED)).thenReturn(8L);
            when(communicationRepository.countByCampaignIdAndStatus(1L, CommunicationStatus.FAILED)).thenReturn(2L);

            Map<String, Object> stats = campaignService.getCampaignAnalytics(1L);

            assertEquals(10L, stats.get("totalRecipients"));
            assertEquals(8L, stats.get("delivered"));
            assertEquals(2L, stats.get("failed"));
            assertEquals(80.0, stats.get("deliveryRate"));
        }
    }
}
