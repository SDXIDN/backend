package com.course.booking.venue;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/venues")
public class VenueController {
    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public List<Venue> getAll(@RequestParam(required = false) String city) {
        return venueService.findAll(city);
    }

    @GetMapping("/{id}")
    public Venue getById(@PathVariable Long id) {
        return venueService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Venue create(@RequestBody Venue body) {
        return venueService.create(body);
    }

    @PutMapping("/{id}")
    public Venue replace(@PathVariable Long id, @RequestBody Venue body) {
        return venueService.replace(id, body);
    }

    @PatchMapping("/{id}")
    public Venue patch(@PathVariable Long id, @RequestBody Venue body) {
        return venueService.patch(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        venueService.delete(id);
    }
}
