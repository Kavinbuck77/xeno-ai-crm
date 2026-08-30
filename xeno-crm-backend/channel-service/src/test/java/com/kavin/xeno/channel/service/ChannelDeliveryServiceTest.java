package com.kavin.xeno.channel.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.kavin.xeno.channel.dto.SendRequest;

@ExtendWith(MockitoExtension.class)
public class ChannelDeliveryServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ChannelDeliveryService channelDeliveryService;

    @Test
    public void testSimulateDelivery_TriggersCallback() {
        ReflectionTestUtils.setField(channelDeliveryService, "crmServiceUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(channelDeliveryService, "simulatedLatencyMs", 10L);

        SendRequest request = new SendRequest();
        request.setCampaignId(1L);
        request.setCustomerId(100L);
        request.setMessage("Test message");
        request.setChannel("EMAIL");

        channelDeliveryService.simulateDelivery(request);

        verify(restTemplate).postForObject(eq("http://localhost:8080/receipts"), any(), eq(String.class));
    }
}
