package com.kavin.xeno.crm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.entity.Order;
import com.kavin.xeno.crm.repository.CustomerRepository;
import com.kavin.xeno.crm.repository.OrderRepository;
import com.kavin.xeno.crm.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    private final Long testUserId = 42L;

    @Test
    public void testSaveOrder_SuccessAndRecalculate() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            Customer customer = new Customer(1L, "Alice", "alice@example.com", "123", 0.0, null, testUserId);
            Order newOrder = new Order();
            newOrder.setCustomerId(1L);
            newOrder.setAmount(150.0);
            newOrder.setOrderDate(LocalDate.of(2026, 8, 20));

            when(customerRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(customer));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order testOrder = new Order();
            testOrder.setId(101L);
            testOrder.setCustomerId(1L);
            testOrder.setAmount(150.0);
            testOrder.setOrderDate(LocalDate.of(2026, 8, 20));
            List<Order> mockOrders = Arrays.asList(testOrder);
            when(orderRepository.findByCustomerId(1L)).thenReturn(mockOrders);

            Order saved = orderService.saveOrder(newOrder);

            assertNotNull(saved);
            assertEquals(150.0, saved.getAmount());
            assertEquals(150.0, customer.getTotalSpent());
            assertEquals(LocalDate.of(2026, 8, 20), customer.getLastOrderDate());

            verify(customerRepository, times(1)).save(customer);
            verify(orderRepository, times(1)).save(newOrder);
        }
    }

    @Test
    public void testSaveOrder_CustomerAccessDenied() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            Order newOrder = new Order();
            newOrder.setCustomerId(1L);
            newOrder.setAmount(100.0);

            when(customerRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> {
                orderService.saveOrder(newOrder);
            });

            verify(orderRepository, never()).save(any(Order.class));
        }
    }

    @Test
    public void testGetOrdersByCustomerId_Success() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUserId).thenReturn(testUserId);

            Customer customer = new Customer(1L, "Alice", "alice@example.com", "123", 100.0, LocalDate.now(), testUserId);
            when(customerRepository.findByIdAndUserId(1L, testUserId)).thenReturn(Optional.of(customer));

            Order testOrder = new Order();
            testOrder.setId(101L);
            testOrder.setCustomerId(1L);
            testOrder.setAmount(100.0);
            List<Order> mockOrders = Collections.singletonList(testOrder);
            when(orderRepository.findByCustomerId(1L)).thenReturn(mockOrders);

            List<Order> result = orderService.getOrdersByCustomerId(1L);

            assertEquals(1, result.size());
            assertEquals(100.0, result.get(0).getAmount());
        }
    }
}
