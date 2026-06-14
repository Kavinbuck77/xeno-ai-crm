package com.kavin.xeno.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kavin.xeno.crm.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}