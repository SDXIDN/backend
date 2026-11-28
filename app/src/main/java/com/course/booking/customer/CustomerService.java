package com.course.booking.customer;

import com.course.booking.booking.BookingRepository;
import com.course.booking.common.ConflictException;
import com.course.booking.common.CrudService;
import com.course.booking.review.ReviewRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerService extends CrudService<Customer> {
    private static final String PHONE_PATTERN = "^\\+?[0-9]{7,15}$";

    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    public CustomerService(CustomerRepository customerRepository, BookingRepository bookingRepository,
                           ReviewRepository reviewRepository) {
        super(customerRepository, "Customer");
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    protected void validate(Customer customer) {
        requireText(customer.getFullName(), "fullName");
        requireText(customer.getEmail(), "email");
        if (!customer.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("email is not a valid address");
        }
        if (customer.getPhone() != null && !customer.getPhone().matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException("phone must be 7-15 digits with optional leading +");
        }
    }

    @Override
    protected void checkConstraints(Customer customer) {
        customerRepository.findByEmail(customer.getEmail())
                .filter(other -> !other.getId().equals(customer.getId()))
                .ifPresent(other -> {
                    throw new ConflictException("Customer with email already exists: " + customer.getEmail());
                });
    }

    @Override
    protected Customer merge(Customer existing, Customer patch) {
        return new Customer(
                orElse(patch.getFullName(), existing.getFullName()),
                orElse(patch.getEmail(), existing.getEmail()),
                orElse(patch.getPhone(), existing.getPhone()));
    }

    @Override
    protected void checkCanDelete(Long id) {
        if (bookingRepository.existsByCustomerId(id) || reviewRepository.existsByCustomerId(id)) {
            throw new ConflictException("Customer " + id + " is referenced by bookings or reviews");
        }
    }
}
