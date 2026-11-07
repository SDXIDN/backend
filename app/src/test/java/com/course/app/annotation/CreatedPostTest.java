package com.course.app.annotation;

import com.course.app.booking.BookingController;
import com.course.app.booking.BookingDto;
import com.course.app.booking.BookingService;
import com.course.app.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CreatedPostTest {

    @RestController
    static class ComposedController {
        @CreatedPost("/composed")
        public String create() {
            return "ok";
        }

        @CreatedPost(path = "/composed-path")
        public String createWithPath() {
            return "ok";
        }
    }

    @RestController
    static class OriginalController {
        @PostMapping("/original")
        @ResponseStatus(HttpStatus.CREATED)
        public String create() {
            return "ok";
        }
    }

    @Mock
    private BookingService bookingService;

    @Test
    void metadata_composedAnnotation_exposesPostMappingAndResponseStatus() throws Exception {
        Method m = ComposedController.class.getMethod("create");

        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(m, RequestMapping.class);
        ResponseStatus rs = AnnotatedElementUtils.findMergedAnnotation(m, ResponseStatus.class);

        assertNotNull(mapping);
        assertArrayEquals(new RequestMethod[]{RequestMethod.POST}, mapping.method());
        assertArrayEquals(new String[]{"/composed"}, mapping.path());
        assertEquals(HttpStatus.CREATED, rs.code());
    }

    @Test
    void metadata_pathAttribute_isAliasedToPostMapping() throws Exception {
        Method m = ComposedController.class.getMethod("createWithPath");

        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(m, RequestMapping.class);

        assertArrayEquals(new String[]{"/composed-path"}, mapping.path());
    }

    @Test
    void request_composedAndOriginal_behaveIdentically() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ComposedController(), new OriginalController()).build();

        mvc.perform(post("/composed")).andExpect(status().isCreated()).andExpect(content().string("ok"));
        mvc.perform(post("/original")).andExpect(status().isCreated()).andExpect(content().string("ok"));
        mvc.perform(post("/composed-path")).andExpect(status().isCreated());
    }

    @Test
    void request_wrongHttpMethod_bothReturn405() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ComposedController(), new OriginalController()).build();

        mvc.perform(get("/composed")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/original")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void bookingController_create_returns201WithBody() throws Exception {
        BookingDto dto = new BookingDto();
        dto.setId(5L);
        dto.setTitle("Haircut");
        when(bookingService.create(any())).thenReturn(dto);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Haircut","customerEmail":"john@example.com",
                         "startTime":"2026-01-10T10:00:00","endTime":"2026-01-10T11:00:00"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void bookingController_create_invalidTimeRange_returns400WithExplanation() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Haircut","customerEmail":"john@example.com",
                         "startTime":"2026-01-10T11:00:00","endTime":"2026-01-10T10:00:00"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("endTime: endTime must be after startTime"));

        verify(bookingService, never()).create(any());
    }
}
