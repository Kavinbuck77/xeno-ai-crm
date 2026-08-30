package com.kavin.xeno.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

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

    @Test
    public void testResolveRecipients_AllCustomers() {
        List<Customer> expected = Arrays.asList(new Customer(), new Customer());
        when(customerRepository.findByUserId(testUserId)).thenReturn(expected);

        List<Customer> result = segmentationService.resolveRecipients(testUserId, "ALL_CUSTOMERS", null);

        assertEquals(2, result.size());
        verify(customerRepository, times(1)).findByUserId(testUserId);
    }

    @Test
    public void testResolveRecipients_DormantCustomers() {
        List<Customer> expected = Arrays.asList(new Customer());
        LocalDate threshold = LocalDate.now().minusDays(180);
        when(customerRepository.findByUserIdAndLastOrderDateBefore(testUserId, threshold)).thenReturn(expected);

        List<Customer> result = segmentationService.resolveRecipients(testUserId, "DORMANT_CUSTOMERS", "{\"daysSinceLastOrder\":180}");

        assertEquals(1, result.size());
        verify(customerRepository, times(1)).findByUserIdAndLastOrderDateBefore(testUserId, threshold);
    }

    @Test
    public void testResolveRecipients_HighValueCustomers() {
        List<Customer> expected = Arrays.asList(new Customer(), new Customer());
        when(customerRepository.findByUserIdAndTotalSpentGreaterThanEqual(testUserId, 1000.0)).thenReturn(expected);

        List<Customer> result = segmentationService.resolveRecipients(testUserId, "HIGH_VALUE_CUSTOMERS", "{\"minTotalSpent\":1000.0}");

        assertEquals(2, result.size());
        verify(customerRepository, times(1)).findByUserIdAndTotalSpentGreaterThanEqual(testUserId, 1000.0);
    }

    @Test
    public void testResolveRecipients_RecentCustomers() {
        List<Customer> expected = Arrays.asList(new Customer());
        LocalDate threshold = LocalDate.now().minusDays(30);
        when(customerRepository.findByUserIdAndLastOrderDateGreaterThanEqual(testUserId, threshold)).thenReturn(expected);

        List<Customer> result = segmentationService.resolveRecipients(testUserId, "RECENT_CUSTOMERS", "{\"daysSinceLastOrder\":30}");

        assertEquals(1, result.size());
        verify(customerRepository, times(1)).findByUserIdAndLastOrderDateGreaterThanEqual(testUserId, threshold);
    }

    @Test
    public void testResolveRecipients_CustomSpendingRange() {
        List<Customer> expected = Arrays.asList(new Customer());
        when(customerRepository.findByUserIdAndTotalSpentBetween(testUserId, 100.0, 500.0)).thenReturn(expected);

        List<Customer> result = segmentationService.resolveRecipients(testUserId, "CUSTOM_SPENDING_RANGE", "{\"minTotalSpent\":100.0, \"maxTotalSpent\":500.0}");

        assertEquals(1, result.size());
        verify(customerRepository, times(1)).findByUserIdAndTotalSpentBetween(testUserId, 100.0, 500.0);
    }
}
