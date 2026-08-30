package com.kavin.xeno.crm.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.repository.CustomerRepository;

@Service
public class SegmentationService {

    private final CustomerRepository customerRepository;

    public SegmentationService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Resolve eligible customers for a campaign based on segment criteria
     */
    public List<Customer> resolveRecipients(Long userId, String segmentType, String criteriaJson) {
        if (segmentType == null) {
            return customerRepository.findByUserId(userId);
        }

        segmentType = segmentType.toUpperCase().trim();

        JsonObject criteriaObj = new JsonObject();
        if (criteriaJson != null && !criteriaJson.trim().isEmpty()) {
            try {
                criteriaObj = JsonParser.parseString(criteriaJson).getAsJsonObject();
            } catch (Exception e) {
                System.err.println("Failed to parse criteria JSON: " + e.getMessage());
            }
        }

        switch (segmentType) {
            case "ALL_CUSTOMERS":
                return customerRepository.findByUserId(userId);

            case "DORMANT_CUSTOMERS": {
                int days = getIntOrDefault(criteriaObj, "daysSinceLastOrder", 180);
                LocalDate thresholdDate = LocalDate.now().minusDays(days);
                return customerRepository.findByUserIdAndLastOrderDateBefore(userId, thresholdDate);
            }

            case "HIGH_VALUE_CUSTOMERS": {
                double minSpent = getDoubleOrDefault(criteriaObj, "minTotalSpent", 500.0);
                return customerRepository.findByUserIdAndTotalSpentGreaterThanEqual(userId, minSpent);
            }

            case "RECENT_CUSTOMERS": {
                int days = getIntOrDefault(criteriaObj, "daysSinceLastOrder", 30);
                LocalDate thresholdDate = LocalDate.now().minusDays(days);
                return customerRepository.findByUserIdAndLastOrderDateGreaterThanEqual(userId, thresholdDate);
            }

            case "CUSTOM_SPENDING_RANGE": {
                double minSpent = getDoubleOrDefault(criteriaObj, "minTotalSpent", 0.0);
                double maxSpent = getDoubleOrDefault(criteriaObj, "maxTotalSpent", Double.MAX_VALUE);
                return customerRepository.findByUserIdAndTotalSpentBetween(userId, minSpent, maxSpent);
            }

            case "CUSTOM_INACTIVITY_RANGE": {
                int minDays = getIntOrDefault(criteriaObj, "minInactivityDays", 0);
                int maxDays = getIntOrDefault(criteriaObj, "maxInactivityDays", 3650);
                LocalDate dateMin = LocalDate.now().minusDays(maxDays);
                LocalDate dateMax = LocalDate.now().minusDays(minDays);
                return customerRepository.findByUserIdAndLastOrderDateBetween(userId, dateMin, dateMax);
            }

            default:
                return customerRepository.findByUserId(userId);
        }
    }

    private int getIntOrDefault(JsonObject obj, String key, int defaultValue) {
        if (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive()) {
            try {
                return obj.get(key).getAsInt();
            } catch (Exception e) {
                // fall through
            }
        }
        return defaultValue;
    }

    private double getDoubleOrDefault(JsonObject obj, String key, double defaultValue) {
        if (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive()) {
            try {
                return obj.get(key).getAsDouble();
            } catch (Exception e) {
                // fall through
            }
        }
        return defaultValue;
    }
}
