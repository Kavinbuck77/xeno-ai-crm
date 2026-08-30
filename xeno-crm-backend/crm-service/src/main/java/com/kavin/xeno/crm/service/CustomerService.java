package com.kavin.xeno.crm.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kavin.xeno.crm.entity.Customer;
import com.kavin.xeno.crm.repository.CustomerRepository;
import com.kavin.xeno.crm.security.SecurityUtils;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer saveCustomer(Customer customer) {
        customer.setUserId(SecurityUtils.getCurrentUserId());
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findByUserId(SecurityUtils.getCurrentUserId());
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findByIdAndUserId(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Customer not found or access denied"));
    }
}