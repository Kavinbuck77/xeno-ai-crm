package com.kavin.xeno.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.repository.CustomerRepository;
import com.kavin.xeno.crm.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private final Long testUserId = 42L;

    @Test
    public void testSaveCustomer() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            Customer customer = new Customer();
            customer.setName("John Doe");
            customer.setEmail("john@example.com");

            when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Customer saved = customerService.saveCustomer(customer);

            assertNotNull(saved);
            assertEquals(testUserId, saved.getUserId());
            assertEquals("John Doe", saved.getName());
            verify(customerRepository, times(1)).save(customer);
        }
    }

    @Test
    public void testGetAllCustomers() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            List<Customer> mockList = Arrays.asList(
                    new Customer(1L, "Alice", "alice@example.com", "123", 100.0, LocalDate.now(), testUserId),
                    new Customer(2L, "Bob", "bob@example.com", "456", 200.0, LocalDate.now(), testUserId)
            );

            when(customerRepository.findByUserId(testUserId)).thenReturn(mockList);

            List<Customer> result = customerService.getAllCustomers();

            assertEquals(2, result.size());
            assertEquals("Alice", result.get(0).getName());
            verify(customerRepository, times(1)).findByUserId(testUserId);
        }
    }

    @Test
    public void testGetCustomerById_Success() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            Customer customer = new Customer(1L, "Alice", "alice@example.com", "123", 100.0, LocalDate.now(), testUserId);
            when(customerRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(customer));

            Customer result = customerService.getCustomerById(1L);

            assertNotNull(result);
            assertEquals("Alice", result.getName());
        }
    }

    @Test
    public void testGetCustomerById_AccessDenied() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            when(customerRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> {
                customerService.getCustomerById(1L);
            });
        }
    }
}
