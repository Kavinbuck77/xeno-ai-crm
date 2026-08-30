package com.kavin.xeno.crm.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.entity.Order;
import com.kavin.xeno.crm.repository.CustomerRepository;
import com.kavin.xeno.crm.repository.OrderRepository;
import com.kavin.xeno.crm.security.SecurityUtils;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Order saveOrder(Order order) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Validate that the customer is owned by the authenticated user
        Customer customer = customerRepository.findByIdAndUserId(order.getCustomerId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found or access denied"));

        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }
        
        Order savedOrder = orderRepository.save(order);

        // Recalculate customer statistics
        List<Order> orders = orderRepository.findByCustomerId(customer.getId());
        double totalSpent = orders.stream().mapToDouble(Order::getAmount).sum();
        LocalDate latestOrderDate = orders.stream()
                .map(Order::getOrderDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        customer.setTotalSpent(totalSpent);
        customer.setLastOrderDate(latestOrderDate);
        customerRepository.save(customer);

        return savedOrder;
    }

    public List<Order> getAllOrders() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Customer> customers = customerRepository.findByUserId(userId);
        List<Long> customerIds = customers.stream().map(Customer::getId).collect(Collectors.toList());

        if (customerIds.isEmpty()) {
            return List.of();
        }
        return orderRepository.findByCustomerIdIn(customerIds);
    }

    public List<Order> getOrdersByCustomerId(Long customerId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Customer customer = customerRepository.findByIdAndUserId(customerId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found or access denied"));
        return orderRepository.findByCustomerId(customer.getId());
    }
}