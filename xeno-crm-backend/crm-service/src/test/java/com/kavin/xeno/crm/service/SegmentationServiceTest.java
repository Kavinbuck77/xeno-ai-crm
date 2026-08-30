package com.kavin.xeno.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
public class SegmentationServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private SegmentationService segmentationService;

    private final Long testUserId = 42L;

    private Customer alice;
    private Customer sarah;

    @BeforeEach
    public void setUp() {
        alice = new Customer(1L, "alice", "alice@test.com", "1234567890", 1000.0, LocalDate.now(), testUserId);
        sarah = new Customer(2L, "SarahJohnson", "sarah@test.com", "0987654321", 1500.0, LocalDate.now(), testUserId);
    }

    @Test
    public void testResolveRecipients_AllCustomers() {
        List<Customer> expected = Arrays.asList(alice, sarah);
        when(customerRepository.findByUserId(testUserId)).thenReturn(expected);

        List<Customer> result = segmentationService.resolveRecipients(testUserId, "ALL_CUSTOMERS", null);

        assertEquals(2, result.size());
        verify(customerRepository, times(1)).findByUserId(testUserId);
    }

    // === REQUIRED USER TEST CASES 1-8 ===

    // Test Case 1: "customers who spent exactly 1000 dollars" -> EQUAL 1000 -> alice only
    @Test
    public void testCase1_Exactly1000_ReturnsAliceOnly() {
        when(customerRepository.findByUserIdAndTotalSpent(testUserId, 1000.0))
                .thenReturn(Collections.singletonList(alice));

        List<Customer> result = segmentationService.resolveRecipients(
                testUserId, "CUSTOM_SPENDING_RANGE", "{\"spendOperator\":\"EQUAL\",\"spendValue\":1000.0}");

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getName());
        verify(customerRepository, times(1)).findByUserIdAndTotalSpent(testUserId, 1000.0);
    }

    // Test Case 2: "customers who spent more than 1000 dollars" -> GREATER_THAN 1000 -> SarahJohnson only
    @Test
    public void testCase2_MoreThan1000_ReturnsSarahOnly() {
        when(customerRepository.findByUserIdAndTotalSpentGreaterThan(testUserId, 1000.0))
                .thenReturn(Collections.singletonList(sarah));

        List<Customer> result = segmentationService.resolveRecipients(
                testUserId, "CUSTOM_SPENDING_RANGE", "{\"spendOperator\":\"GREATER_THAN\",\"spendValue\":1000.0}");

        assertEquals(1, result.size());
        assertEquals("SarahJohnson", result.get(0).getName());
        verify(customerRepository, times(1)).findByUserIdAndTotalSpentGreaterThan(testUserId, 1000.0);
    }

    // Test Case 3: "customers who spent more than 1500 dollars" -> GREATER_THAN 1500 -> 0 recipients
    @Test
    public void testCase3_MoreThan1500_ReturnsZeroRecipients() {
        when(customerRepository.findByUserIdAndTotalSpentGreaterThan(testUserId, 1500.0))
                .thenReturn(Collections.emptyList());

        List<Customer> result = segmentationService.resolveRecipients(
                testUserId, "CUSTOM_SPENDING_RANGE", "{\"spendOperator\":\"GREATER_THAN\",\"spendValue\":1500.0}");

        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(customerRepository, times(1)).findByUserIdAndTotalSpentGreaterThan(testUserId, 1500.0);
    }

    // Test Case 4: "customers who spent at least 1500 dollars" -> GREATER_THAN_OR_EQUAL 1500 -> SarahJohnson only
    @Test
    public void testCase4_AtLeast1500_ReturnsSarahOnly() {
        when(customerRepository.findByUserIdAndTotalSpentGreaterThanEqual(testUserId, 1500.0))
                .thenReturn(Collections.singletonList(sarah));

        List<Customer> result = segmentationService.resolveRecipients(
                testUserId, "HIGH_VALUE_CUSTOMERS", "{\"spendOperator\":\"GREATER_THAN_OR_EQUAL\",\"spendValue\":1500.0}");

        assertEquals(1, result.size());
        assertEquals("SarahJohnson", result.get(0).getName());
        verify(customerRepository, times(1)).findByUserIdAndTotalSpentGreaterThanEqual(testUserId, 1500.0);
    }

    // Test Case 5: "customers who spent less than 1500 dollars" -> LESS_THAN 1500 -> alice only
    @Test
    public void testCase5_LessThan1500_ReturnsAliceOnly() {
        when(customerRepository.findByUserIdAndTotalSpentLessThan(testUserId, 1500.0))
                .thenReturn(Collections.singletonList(alice));

        List<Customer> result = segmentationService.resolveRecipients(
                testUserId, "CUSTOM_SPENDING_RANGE", "{\"spendOperator\":\"LESS_THAN\",\"spendValue\":1500.0}");

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getName());
        verify(customerRepository, times(1)).findByUserIdAndTotalSpentLessThan(testUserId, 1500.0);
    }

    // Test Case 6: "customers who spent at most 1000 dollars" -> LESS_THAN_OR_EQUAL 1000 -> alice only
    @Test
    public void testCase6_AtMost1000_ReturnsAliceOnly() {
        when(customerRepository.findByUserIdAndTotalSpentLessThanEqual(testUserId, 1000.0))
                .thenReturn(Collections.singletonList(alice));

        List<Customer> result = segmentationService.resolveRecipients(
                testUserId, "CUSTOM_SPENDING_RANGE", "{\"spendOperator\":\"LESS_THAN_OR_EQUAL\",\"spendValue\":1000.0}");

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getName());
        verify(customerRepository, times(1)).findByUserIdAndTotalSpentLessThanEqual(testUserId, 1000.0);
    }

    // Test Case 7: "customers who spent exactly 1500 dollars" -> EQUAL 1500 -> SarahJohnson only
    @Test
    public void testCase7_Exactly1500_ReturnsSarahOnly() {
        when(customerRepository.findByUserIdAndTotalSpent(testUserId, 1500.0))
                .thenReturn(Collections.singletonList(sarah));

        List<Customer> result = segmentationService.resolveRecipients(
                testUserId, "EXACT_SPENDING", "{\"spendOperator\":\"EQUAL\",\"spendValue\":1500.0}");

        assertEquals(1, result.size());
        assertEquals("SarahJohnson", result.get(0).getName());
        verify(customerRepository, times(1)).findByUserIdAndTotalSpent(testUserId, 1500.0);
    }

    // Test Case 8: "customers who spent between 1000 and 1500 dollars" -> BETWEEN 1000 and 1500 -> alice + SarahJohnson
    @Test
    public void testCase8_Between1000And1500_ReturnsBoth() {
        when(customerRepository.findByUserIdAndTotalSpentBetween(testUserId, 1000.0, 1500.0))
                .thenReturn(Arrays.asList(alice, sarah));

        List<Customer> result = segmentationService.resolveRecipients(
                testUserId, "CUSTOM_SPENDING_RANGE", "{\"spendOperator\":\"BETWEEN\",\"minSpendValue\":1000.0,\"maxSpendValue\":1500.0}");

        assertEquals(2, result.size());
        verify(customerRepository, times(1)).findByUserIdAndTotalSpentBetween(testUserId, 1000.0, 1500.0);
    }

    @Test
    public void testResolveRecipients_EmptySegment_ReturnsZero() {
        when(customerRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());

        List<Customer> result = segmentationService.resolveRecipients(testUserId, "ALL_CUSTOMERS", null);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testResolveRecipients_NullSegmentType_DefaultsToAll() {
        List<Customer> expected = Arrays.asList(alice);
        when(customerRepository.findByUserId(testUserId)).thenReturn(expected);

        List<Customer> result = segmentationService.resolveRecipients(testUserId, null, null);

        assertEquals(1, result.size());
        verify(customerRepository, times(1)).findByUserId(testUserId);
    }

    @Test
    public void testResolveRecipients_InvalidCriteriaJson_DoesNotThrow() {
        when(customerRepository.findByUserId(testUserId)).thenReturn(Collections.emptyList());

        List<Customer> result = segmentationService.resolveRecipients(
                testUserId, "ALL_CUSTOMERS", "not valid json {{{");

        assertTrue(result.isEmpty());
    }
}
