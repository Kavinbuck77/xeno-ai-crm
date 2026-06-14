package com.kavin.xeno.crm.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class AiCampaignService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model.name:gemini-2.5-flash}")
    private String modelName;

    /**
     * Generate a campaign recommendation using Gemini 2.5 Flash AI
     * 
     * @param goal The marketing goal from the user
     * @return Map containing segment, channel, and message
     */
    public Map<String, String> generateCampaign(String goal) {
        Map<String, String> response = new HashMap<>();

        try {
            // Create the prompt for Gemini
            String prompt = buildCampaignPrompt(goal);

            // Call Gemini API
            String aiResponse = callGeminiAPI(prompt);

            // Parse the response and extract campaign details
            response = parseCampaignResponse(aiResponse);

        } catch (Exception e) {
            // Fallback to default campaign if API call fails
            System.err.println("Error calling Gemini API: " + e.getMessage());
            e.printStackTrace();
            response.put("segment", "All Customers");
            response.put("channel", "WhatsApp");
            response.put("message", "Check out our latest offers.");
        }

        return response;
    }

    /**
     * Build a detailed prompt for campaign generation
     */
    private String buildCampaignPrompt(String goal) {
        return "You are a marketing expert. Based on the following marketing goal, generate a customer campaign strategy.\n\n" +
                "Marketing Goal: " + goal + "\n\n" +
                "Provide the response in exactly this format:\n" +
                "SEGMENT: [target customer segment]\n" +
                "CHANNEL: [preferred communication channel: Email, WhatsApp, SMS, or Push]\n" +
                "MESSAGE: [compelling marketing message - max 100 words]\n\n" +
                "Make the recommendations specific, actionable, and based on best marketing practices.";
    }

    /**
     * Call the Gemini 2.5 Flash API
     */
    private String callGeminiAPI(String prompt) throws Exception {
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" +
                geminiApiKey;

        String requestBody = buildRequestBody(prompt);

        try {
            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            // Send request
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = conn.getResponseCode();
            System.out.println("Gemini API Response Code: " + responseCode);

            if (responseCode != 200) {
                String errorMsg = readError(conn);
                System.err.println("Gemini API error code: " + responseCode + " - " + errorMsg);
                throw new Exception("Gemini API returned error code: " + responseCode);
            }

            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine);
                }
            }

            return extractTextFromJsonResponse(response.toString());

        } catch (Exception e) {
            System.err.println("HTTP request failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Read error message from connection
     */
    private String readError(java.net.HttpURLConnection conn) {
        try {
            java.io.InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(errorStream))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            System.err.println("Error reading error stream: " + e.getMessage());
        }
        return "Unknown error";
    }

    /**
     * Build the request body for Gemini API
     */
    private String buildRequestBody(String prompt) {
        JsonObject requestObj = new JsonObject();

        JsonArray contentsArray = new JsonArray();
        JsonObject contentObj = new JsonObject();

        JsonArray partsArray = new JsonArray();
        JsonObject partObj = new JsonObject();
        partObj.addProperty("text", prompt);
        partsArray.add(partObj);

        contentObj.add("parts", partsArray);
        contentsArray.add(contentObj);

        requestObj.add("contents", contentsArray);

        // Add generation config for better results
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7f);
        generationConfig.addProperty("maxOutputTokens", 500);
        requestObj.add("generationConfig", generationConfig);

        return requestObj.toString();
    }

    /**
     * Extract text from Gemini's JSON response
     */
    private String extractTextFromJsonResponse(String jsonResponse) throws Exception {
        try {
            JsonObject responseObj = JsonParser.parseString(jsonResponse).getAsJsonObject();

            JsonArray candidates = responseObj.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject content = candidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");

                if (parts != null && parts.size() > 0) {
                    JsonObject part = parts.get(0).getAsJsonObject();
                    String text = part.get("text").getAsString();
                    return text;
                }
            }

            throw new Exception("Could not find text in Gemini response");

        } catch (Exception e) {
            System.err.println("Error parsing JSON response: " + e.getMessage());
            System.err.println("Response content: " + jsonResponse);
            throw e;
        }
    }

    /**
     * Parse the Gemini response and extract campaign details
     */
    private Map<String, String> parseCampaignResponse(String aiResponse) {
        Map<String, String> campaign = new HashMap<>();

        try {
            String segment = extractField(aiResponse, "SEGMENT:");
            String channel = extractField(aiResponse, "CHANNEL:");
            String message = extractField(aiResponse, "MESSAGE:");

            campaign.put("segment", !segment.isEmpty() ? segment : "All Customers");
            campaign.put("channel", validateChannel(channel));
            campaign.put("message", !message.isEmpty() ? message : "Check out our latest offers.");

            System.out.println("Generated Campaign - Segment: " + campaign.get("segment") +
                    ", Channel: " + campaign.get("channel") +
                    ", Message: " + campaign.get("message"));

        } catch (Exception e) {
            System.err.println("Error parsing campaign response: " + e.getMessage());
            campaign.put("segment", "All Customers");
            campaign.put("channel", "WhatsApp");
            campaign.put("message", "Check out our latest offers.");
        }

        return campaign;
    }

    /**
     * Extract a field value from the AI response
     */
    private String extractField(String response, String fieldName) {
        int startIndex = response.indexOf(fieldName);
        if (startIndex == -1) {
            return "";
        }

        startIndex += fieldName.length();
        int endIndex = response.indexOf("\n", startIndex);
        if (endIndex == -1) {
            endIndex = response.length();
        }

        return response.substring(startIndex, endIndex).trim();
    }

    /**
     * Validate and normalize the channel
     */
    private String validateChannel(String channel) {
        if (channel.isEmpty()) {
            return "WhatsApp";
        }

        channel = channel.toLowerCase();
        if (channel.contains("email")) {
            return "Email";
        } else if (channel.contains("sms")) {
            return "SMS";
        } else if (channel.contains("push")) {
            return "Push";
        } else if (channel.contains("whatsapp")) {
            return "WhatsApp";
        }

        return "WhatsApp";
    }
}