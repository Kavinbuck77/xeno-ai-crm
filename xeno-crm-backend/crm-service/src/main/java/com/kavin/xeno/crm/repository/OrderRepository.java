package com.kavin.xeno.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kavin.xeno.crm.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}