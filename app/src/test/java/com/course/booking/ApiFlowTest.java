package com.course.booking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowTest {
    @Autowired
    private MockMvc mvc;

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                               String json) throws Exception {
        return mvc.perform(builder.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long create(String path, String json) throws Exception {
        String body = send(post(path), json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long venue() throws Exception {
        return create("/api/v1/venues", "{\"name\":\"Hall\",\"address\":\"Main 1\",\"city\":\"Kyiv\"}");
    }

    private long room(long venueId) throws Exception {
        return create("/api/v1/rooms",
                "{\"name\":\"R1\",\"capacity\":10,\"hourlyPrice\":100.50,\"venueId\":" + venueId + "}");
    }

    private long customer(String email) throws Exception {
        return create("/api/v1/customers", "{\"fullName\":\"Ann\",\"email\":\"" + email + "\"}");
    }

    private String bookingJson(long roomId, long customerId, String start, String end) {
        return "{\"roomId\":" + roomId + ",\"customerId\":" + customerId
                + ",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}";
    }

    @Test
    void venue_fullCrudCycle() throws Exception {
        long id = venue();

        mvc.perform(get("/api/v1/venues/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hall"));

        send(put("/api/v1/venues/" + id), "{\"name\":\"New\",\"address\":\"Addr 2\",\"city\":\"Lviv\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.city").value("Lviv"));

        send(patch("/api/v1/venues/" + id), "{\"city\":\"Odesa\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.city").value("Odesa"));

        mvc.perform(delete("/api/v1/venues/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/venues/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void venue_blankName_returns400() throws Exception {
        send(post("/api/v1/venues"), "{\"name\":\" \",\"address\":\"a\",\"city\":\"c\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void venue_patchWithInvalidValue_returns400AndKeepsStoredData() throws Exception {
        long id = venue();

        send(patch("/api/v1/venues/" + id), "{\"name\":\"\"}").andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/venues/" + id)).andExpect(jsonPath("$.name").value("Hall"));
    }

    @Test
    void venue_malformedJson_returns400() throws Exception {
        send(post("/api/v1/venues"), "{not json").andExpect(status().isBadRequest());
    }

    @Test
    void venue_unknownId_returns404ForAllMethods() throws Exception {
        mvc.perform(get("/api/v1/venues/9999")).andExpect(status().isNotFound());
        send(put("/api/v1/venues/9999"), "{\"name\":\"a\",\"address\":\"b\",\"city\":\"c\"}")
                .andExpect(status().isNotFound());
        send(patch("/api/v1/venues/9999"), "{\"city\":\"c\"}").andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/venues/9999")).andExpect(status().isNotFound());
    }

    @Test
    void venue_filterByCity_returnsOnlyMatching() throws Exception {
        create("/api/v1/venues", "{\"name\":\"X\",\"address\":\"a\",\"city\":\"Zhmerynka\"}");

        mvc.perform(get("/api/v1/venues").param("city", "zhmerynka"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("X"));
    }

    @Test
    void venue_withRooms_cannotBeDeleted() throws Exception {
        long venueId = venue();
        room(venueId);

        mvc.perform(delete("/api/v1/venues/" + venueId)).andExpect(status().isConflict());
    }

    @Test
    void room_unknownVenue_returns404() throws Exception {
        send(post("/api/v1/rooms"), "{\"name\":\"R\",\"capacity\":5,\"hourlyPrice\":10,\"venueId\":8888}")
                .andExpect(status().isNotFound());
    }

    @Test
    void room_invalidCapacityAndPrice_return400() throws Exception {
        long venueId = venue();

        send(post("/api/v1/rooms"), "{\"name\":\"R\",\"capacity\":0,\"hourlyPrice\":10,\"venueId\":" + venueId + "}")
                .andExpect(status().isBadRequest());
        send(post("/api/v1/rooms"), "{\"name\":\"R\",\"capacity\":5,\"hourlyPrice\":-1,\"venueId\":" + venueId + "}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void room_patchCapacity_keepsOtherFields() throws Exception {
        long roomId = room(venue());

        send(patch("/api/v1/rooms/" + roomId), "{\"capacity\":25}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(25))
                .andExpect(jsonPath("$.name").value("R1"));
    }

    @Test
    void room_filterByVenue_returnsOnlyItsRooms() throws Exception {
        long venueId = venue();
        room(venueId);
        room(venue());

        mvc.perform(get("/api/v1/rooms").param("venueId", String.valueOf(venueId)))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void customer_duplicateEmail_returns409() throws Exception {
        customer("dup@example.com");

        send(post("/api/v1/customers"), "{\"fullName\":\"Other\",\"email\":\"DUP@example.com\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void customer_invalidEmailOrPhone_returns400() throws Exception {
        send(post("/api/v1/customers"), "{\"fullName\":\"A\",\"email\":\"nope\"}")
                .andExpect(status().isBadRequest());
        send(post("/api/v1/customers"), "{\"fullName\":\"A\",\"email\":\"ok@example.com\",\"phone\":\"abc\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void customer_putWithOwnEmail_isAllowed() throws Exception {
        long id = customer("same@example.com");

        send(put("/api/v1/customers/" + id), "{\"fullName\":\"Renamed\",\"email\":\"same@example.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Renamed"));
    }

    @Test
    void booking_create_defaultsToPending() throws Exception {
        long roomId = room(venue());
        long customerId = customer("b1@example.com");

        send(post("/api/v1/bookings"), bookingJson(roomId, customerId, "2026-12-01T10:00:00", "2026-12-01T11:00:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void booking_endBeforeStart_returns400() throws Exception {
        long roomId = room(venue());
        long customerId = customer("b2@example.com");

        send(post("/api/v1/bookings"), bookingJson(roomId, customerId, "2026-12-01T11:00:00", "2026-12-01T10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void booking_overlappingTimeInSameRoom_returns409() throws Exception {
        long roomId = room(venue());
        long customerId = customer("b3@example.com");
        create("/api/v1/bookings", bookingJson(roomId, customerId, "2026-12-01T10:00:00", "2026-12-01T12:00:00"));

        send(post("/api/v1/bookings"), bookingJson(roomId, customerId, "2026-12-01T11:00:00", "2026-12-01T13:00:00"))
                .andExpect(status().isConflict());
        send(post("/api/v1/bookings"), bookingJson(roomId, customerId, "2026-12-01T12:00:00", "2026-12-01T13:00:00"))
                .andExpect(status().isCreated());
    }

    @Test
    void booking_cancelledDoesNotBlockRoom() throws Exception {
        long roomId = room(venue());
        long customerId = customer("b4@example.com");
        long bookingId = create("/api/v1/bookings",
                bookingJson(roomId, customerId, "2026-12-02T10:00:00", "2026-12-02T12:00:00"));

        send(patch("/api/v1/bookings/" + bookingId), "{\"status\":\"CANCELLED\"}").andExpect(status().isOk());

        send(post("/api/v1/bookings"), bookingJson(roomId, customerId, "2026-12-02T10:00:00", "2026-12-02T12:00:00"))
                .andExpect(status().isCreated());
    }

    @Test
    void booking_unknownRoomOrCustomer_returns404() throws Exception {
        long roomId = room(venue());
        long customerId = customer("b5@example.com");

        send(post("/api/v1/bookings"), bookingJson(7777, customerId, "2026-12-03T10:00:00", "2026-12-03T11:00:00"))
                .andExpect(status().isNotFound());
        send(post("/api/v1/bookings"), bookingJson(roomId, 7777, "2026-12-03T10:00:00", "2026-12-03T11:00:00"))
                .andExpect(status().isNotFound());
    }

    @Test
    void booking_unknownStatus_returns400() throws Exception {
        long roomId = room(venue());
        long customerId = customer("b6@example.com");

        send(post("/api/v1/bookings"),
                "{\"roomId\":" + roomId + ",\"customerId\":" + customerId
                        + ",\"startTime\":\"2026-12-04T10:00:00\",\"endTime\":\"2026-12-04T11:00:00\",\"status\":\"WRONG\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void roomAndCustomerWithBookings_cannotBeDeleted() throws Exception {
        long roomId = room(venue());
        long customerId = customer("b7@example.com");
        create("/api/v1/bookings", bookingJson(roomId, customerId, "2026-12-05T10:00:00", "2026-12-05T11:00:00"));

        mvc.perform(delete("/api/v1/rooms/" + roomId)).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/customers/" + customerId)).andExpect(status().isConflict());
    }

    @Test
    void booking_deleteThenRoomCanBeDeleted() throws Exception {
        long roomId = room(venue());
        long customerId = customer("b8@example.com");
        long bookingId = create("/api/v1/bookings",
                bookingJson(roomId, customerId, "2026-12-06T10:00:00", "2026-12-06T11:00:00"));

        mvc.perform(delete("/api/v1/bookings/" + bookingId)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/rooms/" + roomId)).andExpect(status().isNoContent());
    }

    @Test
    void review_fullCycleAndFilterByRoom() throws Exception {
        long roomId = room(venue());
        long customerId = customer("r1@example.com");
        long id = create("/api/v1/reviews",
                "{\"roomId\":" + roomId + ",\"customerId\":" + customerId + ",\"rating\":4,\"comment\":\"nice\"}");

        send(patch("/api/v1/reviews/" + id), "{\"rating\":5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("nice"));

        mvc.perform(get("/api/v1/reviews").param("roomId", String.valueOf(roomId)))
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(delete("/api/v1/reviews/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/reviews/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void review_ratingOutOfRange_returns400() throws Exception {
        long roomId = room(venue());
        long customerId = customer("r2@example.com");

        send(post("/api/v1/reviews"),
                "{\"roomId\":" + roomId + ",\"customerId\":" + customerId + ",\"rating\":6}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void review_unknownRoom_returns404() throws Exception {
        long customerId = customer("r3@example.com");

        send(post("/api/v1/reviews"), "{\"roomId\":6666,\"customerId\":" + customerId + ",\"rating\":3}")
                .andExpect(status().isNotFound());
    }
}
