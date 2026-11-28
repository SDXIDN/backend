package com.course.booking.review;

import com.course.booking.common.CrudService;
import com.course.booking.common.NotFoundException;
import com.course.booking.customer.CustomerRepository;
import com.course.booking.room.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService extends CrudService<Review> {
    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;

    public ReviewService(ReviewRepository reviewRepository, RoomRepository roomRepository,
                         CustomerRepository customerRepository) {
        super(reviewRepository, "Review");
        this.reviewRepository = reviewRepository;
        this.roomRepository = roomRepository;
        this.customerRepository = customerRepository;
    }

    public List<Review> findAll(Long roomId) {
        return roomId == null ? findAll() : reviewRepository.findByRoomId(roomId);
    }

    @Override
    protected void validate(Review review) {
        requireNotNull(review.getRoomId(), "roomId");
        requireNotNull(review.getCustomerId(), "customerId");
        requireNotNull(review.getRating(), "rating");
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        if (review.getComment() != null && review.getComment().length() > 1000) {
            throw new IllegalArgumentException("comment must be at most 1000 characters");
        }
    }

    @Override
    protected void checkConstraints(Review review) {
        if (!roomRepository.existsById(review.getRoomId())) {
            throw new NotFoundException("Room not found with id: " + review.getRoomId());
        }
        if (!customerRepository.existsById(review.getCustomerId())) {
            throw new NotFoundException("Customer not found with id: " + review.getCustomerId());
        }
    }

    @Override
    protected Review merge(Review existing, Review patch) {
        return new Review(
                orElse(patch.getRoomId(), existing.getRoomId()),
                orElse(patch.getCustomerId(), existing.getCustomerId()),
                orElse(patch.getRating(), existing.getRating()),
                orElse(patch.getComment(), existing.getComment()));
    }
}
