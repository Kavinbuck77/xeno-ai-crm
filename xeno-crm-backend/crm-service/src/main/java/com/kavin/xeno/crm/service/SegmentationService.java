package com.kavin.xeno.crm.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.entity.SpendOperator;
import com.kavin.xeno.crm.repository.CustomerRepository;

@Service
public class SegmentationService {

    private static final Logger log = LoggerFactory.getLogger(SegmentationService.class);

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
                log.warn("Failed to parse criteria JSON: {}", e.getMessage());
            }
        }

        // Check if explicit spendOperator is specified in criteria
        SpendOperator spendOperator = getSpendOperator(criteriaObj);

        // If spendOperator is explicitly specified, execute spending query based on spendOperator
        if (spendOperator != null) {
            return resolveBySpendOperator(userId, spendOperator, criteriaObj);
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
                log.info("HIGH_VALUE_CUSTOMERS segment: selecting customers with totalSpent >= {}", minSpent);
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
                log.info("CUSTOM_SPENDING_RANGE segment: selecting customers with {} <= totalSpent <= {}", minSpent, maxSpent);
                return customerRepository.findByUserIdAndTotalSpentBetween(userId, minSpent, maxSpent);
            }

            case "EXACT_SPENDING": {
                double exactSpent = getDoubleOrDefault(criteriaObj, "exactTotalSpent", 0.0);
                if (!hasKey(criteriaObj, "exactTotalSpent") && hasKey(criteriaObj, "spendValue")) {
                    exactSpent = getDoubleOrDefault(criteriaObj, "spendValue", 0.0);
                }
                log.info("EXACT_SPENDING segment: selecting customers with totalSpent == {}", exactSpent);
                return customerRepository.findByUserIdAndTotalSpent(userId, exactSpent);
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

    private SpendOperator getSpendOperator(JsonObject criteriaObj) {
        if (criteriaObj == null) {
            return null;
        }
        if (criteriaObj.has("spendOperator") && criteriaObj.get("spendOperator").isJsonPrimitive()) {
            return SpendOperator.fromString(criteriaObj.get("spendOperator").getAsString());
        }
        return null;
    }

    private List<Customer> resolveBySpendOperator(Long userId, SpendOperator operator, JsonObject criteriaObj) {
        double primaryVal = getPrimarySpendValue(criteriaObj);

        switch (operator) {
            case GREATER_THAN:
                log.info("Executing spend filter: totalSpent > {}", primaryVal);
                return customerRepository.findByUserIdAndTotalSpentGreaterThan(userId, primaryVal);

            case GREATER_THAN_OR_EQUAL:
                log.info("Executing spend filter: totalSpent >= {}", primaryVal);
                return customerRepository.findByUserIdAndTotalSpentGreaterThanEqual(userId, primaryVal);

            case LESS_THAN:
                log.info("Executing spend filter: totalSpent < {}", primaryVal);
                return customerRepository.findByUserIdAndTotalSpentLessThan(userId, primaryVal);

            case LESS_THAN_OR_EQUAL:
                log.info("Executing spend filter: totalSpent <= {}", primaryVal);
                return customerRepository.findByUserIdAndTotalSpentLessThanEqual(userId, primaryVal);

            case EQUAL:
                log.info("Executing spend filter: totalSpent == {}", primaryVal);
                return customerRepository.findByUserIdAndTotalSpent(userId, primaryVal);

            case BETWEEN:
                double minVal = getDoubleOrDefault(criteriaObj, "minSpendValue", primaryVal);
                if (!hasKey(criteriaObj, "minSpendValue") && hasKey(criteriaObj, "minTotalSpent")) {
                    minVal = getDoubleOrDefault(criteriaObj, "minTotalSpent", 0.0);
                }
                double maxVal = getDoubleOrDefault(criteriaObj, "maxSpendValue", Double.MAX_VALUE);
                if (!hasKey(criteriaObj, "maxSpendValue") && hasKey(criteriaObj, "maxTotalSpent")) {
                    maxVal = getDoubleOrDefault(criteriaObj, "maxTotalSpent", Double.MAX_VALUE);
                }
                log.info("Executing spend filter: {} <= totalSpent <= {}", minVal, maxVal);
                return customerRepository.findByUserIdAndTotalSpentBetween(userId, minVal, maxVal);

            default:
                log.warn("Unknown spend operator {}, returning user customers", operator);
                return customerRepository.findByUserId(userId);
        }
    }

    private double getPrimarySpendValue(JsonObject criteriaObj) {
        if (hasKey(criteriaObj, "spendValue")) {
            return getDoubleOrDefault(criteriaObj, "spendValue", 0.0);
        }
        if (hasKey(criteriaObj, "exactTotalSpent")) {
            return getDoubleOrDefault(criteriaObj, "exactTotalSpent", 0.0);
        }
        if (hasKey(criteriaObj, "minTotalSpent")) {
            return getDoubleOrDefault(criteriaObj, "minTotalSpent", 0.0);
        }
        if (hasKey(criteriaObj, "maxTotalSpent")) {
            return getDoubleOrDefault(criteriaObj, "maxTotalSpent", 0.0);
        }
        return 0.0;
    }

    private boolean hasKey(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull();
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
