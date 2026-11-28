package com.course.booking.review;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<Review> getAll(@RequestParam(required = false) Long roomId) {
        return reviewService.findAll(roomId);
    }

    @GetMapping("/{id}")
    public Review getById(@PathVariable Long id) {
        return reviewService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Review create(@RequestBody Review body) {
        return reviewService.create(body);
    }

    @PutMapping("/{id}")
    public Review replace(@PathVariable Long id, @RequestBody Review body) {
        return reviewService.replace(id, body);
    }

    @PatchMapping("/{id}")
    public Review patch(@PathVariable Long id, @RequestBody Review body) {
        return reviewService.patch(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        reviewService.delete(id);
    }
}
