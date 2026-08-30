package com.kavin.xeno.crm.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kavin.xeno.crm.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByUserId(Long userId);

    Optional<Customer> findByIdAndUserId(Long id, Long userId);

    List<Customer> findByUserIdAndLastOrderDateBefore(Long userId, LocalDate thresholdDate);

    List<Customer> findByUserIdAndTotalSpentGreaterThan(Long userId, Double totalSpent);

    List<Customer> findByUserIdAndTotalSpentGreaterThanEqual(Long userId, Double minTotalSpent);

    List<Customer> findByUserIdAndTotalSpentLessThan(Long userId, Double totalSpent);

    List<Customer> findByUserIdAndTotalSpentLessThanEqual(Long userId, Double totalSpent);

    List<Customer> findByUserIdAndLastOrderDateGreaterThanEqual(Long userId, LocalDate thresholdDate);

    List<Customer> findByUserIdAndTotalSpentBetween(Long userId, Double minTotalSpent, Double maxTotalSpent);

    List<Customer> findByUserIdAndLastOrderDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    List<Customer> findByUserIdAndTotalSpent(Long userId, Double totalSpent);
}