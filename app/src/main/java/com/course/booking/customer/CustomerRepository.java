package com.course.booking.customer;

import com.course.booking.common.InMemoryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CustomerRepository extends InMemoryRepository<Customer> {
    public Optional<Customer> findByEmail(String email) {
        return findAll().stream().filter(c -> c.getEmail() != null && c.getEmail().equalsIgnoreCase(email)).findFirst();
    }
}
