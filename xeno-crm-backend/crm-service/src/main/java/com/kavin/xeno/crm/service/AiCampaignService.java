package com.kavin.xeno.crm.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.exception.AiGenerationException;
import com.kavin.xeno.crm.security.SecurityUtils;

@Service
public class AiCampaignService {

    private static final Logger log = LoggerFactory.getLogger(AiCampaignService.class);

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model.name:gemini-3.6-flash}")
    private String modelName;

    private final SegmentationService segmentationService;
    private final RestTemplate restTemplate;
    private final Gson gson = new Gson();

    private static final List<String> ALLOWED_SEGMENTS = List.of(
            "ALL_CUSTOMERS",
            "DORMANT_CUSTOMERS",
            "HIGH_VALUE_CUSTOMERS",
            "RECENT_CUSTOMERS",
            "CUSTOM_SPENDING_RANGE",
            "CUSTOM_INACTIVITY_RANGE"
    );

    private static final List<String> ALLOWED_CHANNELS = List.of("EMAIL", "WHATSAPP", "SMS", "PUSH");

    public AiCampaignService(SegmentationService segmentationService, RestTemplate restTemplate) {
        this.segmentationService = segmentationService;
        this.restTemplate = restTemplate;
    }

    /**
     * Generate a campaign recommendation using Gemini AI
     */
    public Map<String, Object> generateCampaign(String goal) {
        if (goal == null || goal.trim().isEmpty()) {
            throw new IllegalArgumentException("Campaign goal must not be empty");
        }

        try {
            // First attempt
            String prompt = buildCampaignPrompt(goal);
            Map<String, Object> strategy = attemptGeneration(prompt);

            if (strategy == null) {
                // Retry once with a stricter compact prompt
                log.warn("First Gemini attempt failed or returned invalid JSON. Retrying with compact prompt.");
                String retryPrompt = buildCompactRetryPrompt(goal);
                strategy = attemptGeneration(retryPrompt);
            }

            if (strategy == null) {
                throw new AiGenerationException("AI campaign generation failed. Please try again.");
            }

            // Resolve recipients through the existing segmentation service
            Long userId = SecurityUtils.getCurrentUserId();
            String segmentType = (String) strategy.get("segmentType");
            String criteriaJson = gson.toJson(strategy.get("criteria"));
            List<Customer> recipients = segmentationService.resolveRecipients(userId, segmentType, criteriaJson);

            strategy.put("recipientCount", recipients.size());

            log.info("Campaign generated successfully: segment={}, channel={}, recipients={}",
                    segmentType, strategy.get("channel"), recipients.size());

            return strategy;

        } catch (AiGenerationException e) {
            throw e; // Already a clean user-facing error
        } catch (Exception e) {
            log.error("Unexpected error during campaign generation: {}", e.getMessage(), e);
            throw new AiGenerationException("AI campaign generation failed. Please try again.");
        }
    }

    /**
     * Attempt a single Gemini API call and parse the response.
     * Returns null if the response is invalid/unparseable (instead of throwing).
     */
    private Map<String, Object> attemptGeneration(String prompt) {
        try {
            String aiResponse = callGeminiAPI(prompt);
            return parseAndValidateCampaignResponse(aiResponse);
        } catch (Exception e) {
            log.warn("Gemini attempt failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildCampaignPrompt(String goal) {
        return """
                You are a CRM campaign strategist.

                Analyze this marketing goal and return ONE valid JSON object.

                IMPORTANT:
                - Return ONLY JSON.
                - Do NOT use markdown.
                - Do NOT use ```json.
                - Do NOT add explanations before or after the JSON.
                - The JSON must be complete and syntactically valid.
                - Keep the campaign name short (under 5 words).
                - Keep the message under 40 words.

                Allowed segmentType values:
                ALL_CUSTOMERS
                DORMANT_CUSTOMERS
                HIGH_VALUE_CUSTOMERS
                RECENT_CUSTOMERS
                CUSTOM_SPENDING_RANGE
                CUSTOM_INACTIVITY_RANGE

                Allowed channel values:
                EMAIL
                WHATSAPP
                SMS
                PUSH

                JSON format:

                {
                  "name": "Short campaign name",
                  "segmentType": "DORMANT_CUSTOMERS",
                  "criteria": {
                    "daysSinceLastOrder": 180
                  },
                  "channel": "EMAIL",
                  "message": "We miss you! Get 20% off your next order."
                }

                Marketing goal:
                """ + goal;
    }

    private String buildCompactRetryPrompt(String goal) {
        return "Return ONLY a compact single-line JSON object with keys: name, segmentType, criteria, channel, message. " +
                "segmentType must be one of: ALL_CUSTOMERS, DORMANT_CUSTOMERS, HIGH_VALUE_CUSTOMERS, RECENT_CUSTOMERS, CUSTOM_SPENDING_RANGE, CUSTOM_INACTIVITY_RANGE. " +
                "channel must be one of: EMAIL, WHATSAPP, SMS, PUSH. " +
                "criteria is an object with relevant numeric thresholds. " +
                "No markdown. No explanation. Only JSON. " +
                "Goal: " + goal;
    }

    private String callGeminiAPI(String prompt) throws Exception {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || geminiApiKey.contains("your-api-key-here")) {
            throw new AiGenerationException("Gemini API key is not configured. Set the GEMINI_API_KEY environment variable.");
        }

        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelName
                + ":generateContent?key="
                + geminiApiKey;

        String requestBody = buildRequestBody(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        log.debug("Calling Gemini API with model: {}", modelName);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("Gemini API returned HTTP {}", response.getStatusCode().value());
                throw new Exception("Gemini API returned error code " + response.getStatusCode().value());
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.error("Gemini API returned empty response body");
                throw new Exception("Gemini API returned empty response");
            }

            // Check for finishReason to detect truncation
            try {
                JsonObject fullResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray candidates = fullResponse.getAsJsonArray("candidates");
                if (candidates != null && candidates.size() > 0) {
                    JsonObject candidate = candidates.get(0).getAsJsonObject();
                    if (candidate.has("finishReason")) {
                        String finishReason = candidate.get("finishReason").getAsString();
                        if ("MAX_TOKENS".equals(finishReason)) {
                            log.warn("Gemini response was truncated (finishReason=MAX_TOKENS)");
                        }
                        log.debug("Gemini finishReason: {}", finishReason);
                    }
                }
            } catch (Exception parseCheck) {
                log.debug("Could not check finishReason: {}", parseCheck.getMessage());
            }

            return extractTextFromJsonResponse(responseBody);

        } catch (HttpStatusCodeException e) {
            log.error("Gemini API HTTP error {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new Exception("Gemini API returned error " + e.getStatusCode().value());
        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new Exception("Gemini API request failed: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String prompt) {
        JsonObject requestObj = new JsonObject();

        // Contents
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObj = new JsonObject();
        JsonArray partsArray = new JsonArray();
        JsonObject partObj = new JsonObject();
        partObj.addProperty("text", prompt);
        partsArray.add(partObj);
        contentObj.add("parts", partsArray);
        contentsArray.add(contentObj);
        requestObj.add("contents", contentsArray);

        // Generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.2);
        generationConfig.addProperty("maxOutputTokens", 1024);
        generationConfig.addProperty("responseMimeType", "application/json");

        // Response schema — forces Gemini to produce valid JSON matching this exact structure
        JsonObject responseSchema = buildResponseSchema();
        generationConfig.add("responseSchema", responseSchema);

        requestObj.add("generationConfig", generationConfig);

        return requestObj.toString();
    }

    /**
     * Build a JSON Schema (OpenAPI 3.0 subset) that Gemini uses to constrain its output.
     * This eliminates truncation and malformed JSON issues by making the model
     * produce structurally valid output that matches the campaign data model.
     */
    private JsonObject buildResponseSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "OBJECT");

        JsonObject properties = new JsonObject();

        // name
        JsonObject nameProp = new JsonObject();
        nameProp.addProperty("type", "STRING");
        nameProp.addProperty("description", "Short campaign name, under 5 words");
        properties.add("name", nameProp);

        // segmentType
        JsonObject segmentProp = new JsonObject();
        segmentProp.addProperty("type", "STRING");
        JsonArray segmentEnum = new JsonArray();
        ALLOWED_SEGMENTS.forEach(segmentEnum::add);
        segmentProp.add("enum", segmentEnum);
        properties.add("segmentType", segmentProp);

        // criteria
        JsonObject criteriaProp = new JsonObject();
        criteriaProp.addProperty("type", "OBJECT");
        // criteria properties
        JsonObject criteriaProperties = new JsonObject();

        JsonObject daysSince = new JsonObject();
        daysSince.addProperty("type", "INTEGER");
        daysSince.addProperty("description", "Days since last order for dormant/recent segments");
        criteriaProperties.add("daysSinceLastOrder", daysSince);

        JsonObject minSpent = new JsonObject();
        minSpent.addProperty("type", "NUMBER");
        minSpent.addProperty("description", "Minimum total spent for high value segments");
        criteriaProperties.add("minTotalSpent", minSpent);

        JsonObject maxSpent = new JsonObject();
        maxSpent.addProperty("type", "NUMBER");
        maxSpent.addProperty("description", "Maximum total spent for spending range segments");
        criteriaProperties.add("maxTotalSpent", maxSpent);

        JsonObject minInactivity = new JsonObject();
        minInactivity.addProperty("type", "INTEGER");
        minInactivity.addProperty("description", "Minimum inactivity days");
        criteriaProperties.add("minInactivityDays", minInactivity);

        JsonObject maxInactivity = new JsonObject();
        maxInactivity.addProperty("type", "INTEGER");
        maxInactivity.addProperty("description", "Maximum inactivity days");
        criteriaProperties.add("maxInactivityDays", maxInactivity);

        criteriaProp.add("properties", criteriaProperties);
        properties.add("criteria", criteriaProp);

        // channel
        JsonObject channelProp = new JsonObject();
        channelProp.addProperty("type", "STRING");
        JsonArray channelEnum = new JsonArray();
        ALLOWED_CHANNELS.forEach(channelEnum::add);
        channelProp.add("enum", channelEnum);
        properties.add("channel", channelProp);

        // message
        JsonObject messageProp = new JsonObject();
        messageProp.addProperty("type", "STRING");
        messageProp.addProperty("description", "Marketing message, under 40 words");
        properties.add("message", messageProp);

        schema.add("properties", properties);

        // Required fields
        JsonArray required = new JsonArray();
        required.add("name");
        required.add("segmentType");
        required.add("channel");
        required.add("message");
        schema.add("required", required);

        return schema;
    }

    private String extractTextFromJsonResponse(String jsonResponse) throws Exception {
        JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonArray candidates = responseObj.getAsJsonArray("candidates");
        if (candidates != null && candidates.size() > 0) {
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            JsonObject content = candidate.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");

            if (parts != null && parts.size() > 0) {
                JsonObject part = parts.get(0).getAsJsonObject();
                String text = part.get("text").getAsString();
                log.debug("Raw Gemini text response length: {} chars", text.length());
                return text;
            }
        }
        throw new Exception("Could not parse text candidate from Gemini response");
    }

    /**
     * Sanitize raw text from Gemini that may contain markdown fences or extra text,
     * then parse and validate against the campaign schema.
     */
    private Map<String, Object> parseAndValidateCampaignResponse(String rawText) throws Exception {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new Exception("Gemini returned empty text response");
        }

        // Sanitize: remove markdown fences if present
        String sanitized = sanitizeJsonResponse(rawText);

        log.debug("Sanitized Gemini response: {}", sanitized.length() > 500
                ? sanitized.substring(0, 500) + "..." : sanitized);

        // Parse JSON
        JsonObject responseObj;
        try {
            responseObj = JsonParser.parseString(sanitized).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            log.error("Failed to parse Gemini response as JSON. Raw text (first 300 chars): {}",
                    rawText.length() > 300 ? rawText.substring(0, 300) : rawText);
            throw new Exception("Gemini returned invalid JSON: " + e.getMessage());
        }

        // Extract and validate fields
        String name = getStringOrDefault(responseObj, "name", "AI Campaign");
        String segmentType = getStringOrDefault(responseObj, "segmentType", "ALL_CUSTOMERS");
        String channel = getStringOrDefault(responseObj, "channel", "EMAIL");
        String message = getStringOrDefault(responseObj, "message", "");

        // Validate segmentType
        segmentType = segmentType.toUpperCase().trim();
        if (!ALLOWED_SEGMENTS.contains(segmentType)) {
            log.warn("Invalid segmentType from Gemini: '{}', defaulting to ALL_CUSTOMERS", segmentType);
            segmentType = "ALL_CUSTOMERS";
        }

        // Validate channel
        channel = channel.toUpperCase().trim();
        if (!ALLOWED_CHANNELS.contains(channel)) {
            log.warn("Invalid channel from Gemini: '{}', defaulting to EMAIL", channel);
            channel = "EMAIL";
        }

        // Validate message is not empty
        if (message.trim().isEmpty()) {
            log.warn("Gemini returned empty message, using default");
            message = "Check out our latest offer!";
        }

        // Parse criteria
        Map<String, Object> criteria = new HashMap<>();
        if (responseObj.has("criteria") && responseObj.get("criteria").isJsonObject()) {
            JsonObject criteriaObj = responseObj.getAsJsonObject("criteria");
            for (String key : criteriaObj.keySet()) {
                if (criteriaObj.get(key).isJsonPrimitive()) {
                    if (criteriaObj.get(key).getAsJsonPrimitive().isNumber()) {
                        criteria.put(key, criteriaObj.get(key).getAsDouble());
                    } else if (criteriaObj.get(key).getAsJsonPrimitive().isBoolean()) {
                        criteria.put(key, criteriaObj.get(key).getAsBoolean());
                    } else {
                        criteria.put(key, criteriaObj.get(key).getAsString());
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("segmentType", segmentType);
        result.put("criteria", criteria);
        result.put("channel", channel);
        result.put("message", message);

        return result;
    }

    /**
     * Sanitize Gemini output: strip markdown fences, trim whitespace,
     * and extract the JSON object if surrounded by other text.
     */
    private String sanitizeJsonResponse(String raw) {
        String text = raw.trim();

        // Remove markdown code fences: ```json ... ``` or ``` ... ```
        if (text.startsWith("```")) {
            // Remove opening fence (with optional language tag)
            int firstNewline = text.indexOf('\n');
            if (firstNewline > 0) {
                text = text.substring(firstNewline + 1);
            }
            // Remove closing fence
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }

        // If the text doesn't start with '{', try to find the JSON object
        if (!text.startsWith("{")) {
            int braceStart = text.indexOf('{');
            if (braceStart >= 0) {
                text = text.substring(braceStart);
            }
        }

        // If the text has trailing garbage after the JSON object, trim it
        if (text.startsWith("{")) {
            int braceCount = 0;
            int endIndex = -1;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '{') braceCount++;
                else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endIndex = i;
                        break;
                    }
                }
            }
            if (endIndex > 0 && endIndex < text.length() - 1) {
                text = text.substring(0, endIndex + 1);
            }
        }

        return text.trim();
    }

    private String getStringOrDefault(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsString();
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}