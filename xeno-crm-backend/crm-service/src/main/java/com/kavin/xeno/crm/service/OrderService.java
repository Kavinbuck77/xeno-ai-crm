package com.kavin.xeno.crm.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kavin.xeno.crm.entity.Order;
import com.kavin.xeno.crm.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}