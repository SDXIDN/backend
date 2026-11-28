package com.course.booking.room;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<Room> getAll(@RequestParam(required = false) Long venueId) {
        return roomService.findAll(venueId);
    }

    @GetMapping("/{id}")
    public Room getById(@PathVariable Long id) {
        return roomService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Room create(@RequestBody Room body) {
        return roomService.create(body);
    }

    @PutMapping("/{id}")
    public Room replace(@PathVariable Long id, @RequestBody Room body) {
        return roomService.replace(id, body);
    }

    @PatchMapping("/{id}")
    public Room patch(@PathVariable Long id, @RequestBody Room body) {
        return roomService.patch(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        roomService.delete(id);
    }
}
