package com.kavin.xeno.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.exception.AiGenerationException;
import com.kavin.xeno.crm.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
public class AiCampaignServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SegmentationService segmentationService;

    @InjectMocks
    private AiCampaignService aiCampaignService;

    private final Long testUserId = 42L;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(aiCampaignService, "geminiApiKey", "mock-api-key");
        ReflectionTestUtils.setField(aiCampaignService, "modelName", "gemini-3.6-flash");
    }

    @Test
    public void testGenerateCampaign_Success() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            String mockGeminiResponse = "{\n" +
                    "  \"candidates\": [{\n" +
                    "    \"content\": {\n" +
                    "      \"parts\": [{\n" +
                    "        \"text\": \"{\\n  \\\"name\\\": \\\"Dormant Re-engagement\\\",\\n  \\\"segmentType\\\": \\\"DORMANT_CUSTOMERS\\\",\\n  \\\"criteria\\\": {\\n    \\\"daysSinceLastOrder\\\": 180\\n  },\\n  \\\"channel\\\": \\\"EMAIL\\\",\\n  \\\"message\\\": \\\"We miss you!\\\"\\n}\"\n" +
                    "      }]\n" +
                    "    },\n" +
                    "    \"finishReason\": \"STOP\"\n" +
                    "  }]\n" +
                    "}";

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(mockGeminiResponse, HttpStatus.OK));

            when(segmentationService.resolveRecipients(eq(testUserId), eq("DORMANT_CUSTOMERS"), anyString()))
                    .thenReturn(Collections.singletonList(new Customer()));

            Map<String, Object> result = aiCampaignService.generateCampaign("bring back inactive customers");

            assertNotNull(result);
            assertEquals("Dormant Re-engagement", result.get("name"));
            assertEquals("DORMANT_CUSTOMERS", result.get("segmentType"));
            assertEquals("EMAIL", result.get("channel"));
            assertEquals("We miss you!", result.get("message"));
            assertEquals(1, result.get("recipientCount"));
        }
    }

    @Test
    public void testGenerateCampaign_HandlesMarkdownFences() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            // Simulate Gemini wrapping JSON in markdown code fences
            String fencedJson = "```json\n{\"name\":\"Promo\",\"segmentType\":\"ALL_CUSTOMERS\",\"criteria\":{},\"channel\":\"SMS\",\"message\":\"Hi!\"}\n```";
            String mockGeminiResponse = "{\n" +
                    "  \"candidates\": [{\n" +
                    "    \"content\": {\n" +
                    "      \"parts\": [{\"text\": " + escapeJsonString(fencedJson) + "}]\n" +
                    "    },\n" +
                    "    \"finishReason\": \"STOP\"\n" +
                    "  }]\n" +
                    "}";

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(mockGeminiResponse, HttpStatus.OK));

            when(segmentationService.resolveRecipients(eq(testUserId), eq("ALL_CUSTOMERS"), anyString()))
                    .thenReturn(Collections.singletonList(new Customer()));

            Map<String, Object> result = aiCampaignService.generateCampaign("send a promo to everyone");

            assertNotNull(result);
            assertEquals("Promo", result.get("name"));
            assertEquals("ALL_CUSTOMERS", result.get("segmentType"));
            assertEquals("SMS", result.get("channel"));
        }
    }

    @Test
    public void testGenerateCampaign_GeminiApiFailure() {
        // Both attempts will fail, so the service should throw AiGenerationException
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(AiGenerationException.class, () -> {
            aiCampaignService.generateCampaign("bring back inactive customers");
        });
    }

    @Test
    public void testGenerateCampaign_MissingApiKey() {
        ReflectionTestUtils.setField(aiCampaignService, "geminiApiKey", "");

        assertThrows(AiGenerationException.class, () -> {
            aiCampaignService.generateCampaign("bring back inactive customers");
        });
    }

    @Test
    public void testGenerateCampaign_InvalidSegmentDefaultsToAll() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            // Return invalid segmentType from Gemini
            String invalidSegmentJson = "{\"name\":\"Test\",\"segmentType\":\"INVALID_SEGMENT\",\"criteria\":{},\"channel\":\"EMAIL\",\"message\":\"Hello!\"}";
            String mockGeminiResponse = "{\n" +
                    "  \"candidates\": [{\n" +
                    "    \"content\": {\n" +
                    "      \"parts\": [{\"text\": " + escapeJsonString(invalidSegmentJson) + "}]\n" +
                    "    },\n" +
                    "    \"finishReason\": \"STOP\"\n" +
                    "  }]\n" +
                    "}";

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(mockGeminiResponse, HttpStatus.OK));

            when(segmentationService.resolveRecipients(eq(testUserId), eq("ALL_CUSTOMERS"), anyString()))
                    .thenReturn(Collections.singletonList(new Customer()));

            Map<String, Object> result = aiCampaignService.generateCampaign("test goal");

            assertEquals("ALL_CUSTOMERS", result.get("segmentType"));
        }
    }

    private String escapeJsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
