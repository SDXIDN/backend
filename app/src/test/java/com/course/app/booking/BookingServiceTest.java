// package com.course.app.booking;

// import jakarta.persistence.EntityNotFoundException;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertSame;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyLong;
// import static org.mockito.Mockito.never;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// @ExtendWith(MockitoExtension.class)
// class BookingServiceTest {

//     private static final LocalDateTime START = LocalDateTime.of(2026, 12, 1, 10, 0);
//     private static final LocalDateTime END = LocalDateTime.of(2026, 12, 1, 11, 0);

//     @Mock
//     private BookingRepository bookingRepository;

//     @Mock
//     private BookingMapper bookingMapper;

//     @InjectMocks
//     private BookingService bookingService;

//     @Test
//     void findAll_bookingsExist_returnsMappedDtosInOrder() {
//         BookingEntity first = entity(1L, "First");
//         BookingEntity second = entity(2L, "Second");
//         BookingDto firstDto = dto(1L, "First");
//         BookingDto secondDto = dto(2L, "Second");
//         when(bookingRepository.findAll()).thenReturn(List.of(first, second));
//         when(bookingMapper.toDto(first)).thenReturn(firstDto);
//         when(bookingMapper.toDto(second)).thenReturn(secondDto);

//         List<BookingDto> result = bookingService.findAll();

//         assertThat(result).containsExactly(firstDto, secondDto);
//         verify(bookingRepository).findAll();
//     }

//     @Test
//     void findAll_noBookings_returnsEmptyListAndNeverUsesMapper() {
//         when(bookingRepository.findAll()).thenReturn(List.of());

//         List<BookingDto> result = bookingService.findAll();

//         assertTrue(result.isEmpty());
//         verify(bookingMapper, never()).toDto(any(BookingEntity.class));
//     }

//     @Test
//     void findAll_repositoryFails_propagatesException() {
//         when(bookingRepository.findAll()).thenThrow(new IllegalStateException("db down"));

//         IllegalStateException ex = assertThrows(IllegalStateException.class, () -> bookingService.findAll());
//         assertEquals("db down", ex.getMessage());
//     }

//     @Test
//     void create_validRequest_savesMappedEntityAndReturnsDto() {
//         CreateBookingRequest request = request("Team sync");
//         BookingEntity toSave = entity(null, "Team sync");
//         BookingEntity saved = entity(10L, "Team sync");
//         BookingDto expected = dto(10L, "Team sync");
//         when(bookingMapper.toEntity(request)).thenReturn(toSave);
//         when(bookingRepository.save(toSave)).thenReturn(saved);
//         when(bookingMapper.toDto(saved)).thenReturn(expected);

//         BookingDto result = bookingService.create(request);

//         assertEquals(10L, result.getId());
//         assertEquals("Team sync", result.getTitle());
//         ArgumentCaptor<BookingEntity> captor = ArgumentCaptor.forClass(BookingEntity.class);
//         verify(bookingRepository).save(captor.capture());
//         assertSame(toSave, captor.getValue());
//         assertEquals(BookingStatus.PENDING, captor.getValue().getStatus());
//     }

//     @Test
//     void create_repositoryFails_propagatesExceptionAndDoesNotMapResult() {
//         CreateBookingRequest request = request("Team sync");
//         BookingEntity toSave = entity(null, "Team sync");
//         when(bookingMapper.toEntity(request)).thenReturn(toSave);
//         when(bookingRepository.save(toSave)).thenThrow(new IllegalStateException("constraint violated"));

//         assertThrows(IllegalStateException.class, () -> bookingService.create(request));
//         verify(bookingMapper, never()).toDto(any(BookingEntity.class));
//     }

//     @Test
//     void update_existingId_mapsRequestOntoEntityAndSaves() {
//         CreateBookingRequest request = request("Updated");
//         BookingEntity existing = entity(5L, "Old");
//         BookingDto expected = dto(5L, "Updated");
//         when(bookingRepository.findById(5L)).thenReturn(Optional.of(existing));
//         when(bookingRepository.save(existing)).thenReturn(existing);
//         when(bookingMapper.toDto(existing)).thenReturn(expected);

//         BookingDto result = bookingService.update(5L, request);

//         assertSame(expected, result);
//         verify(bookingMapper).updateEntity(request, existing);
//         verify(bookingRepository).save(existing);
//     }

//     @Test
//     void update_unknownId_throwsEntityNotFoundAndDoesNotSave() {
//         CreateBookingRequest request = request("Updated");
//         when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

//         EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
//                 () -> bookingService.update(99L, request));

//         assertTrue(ex.getMessage().contains("99"));
//         verify(bookingMapper, never()).updateEntity(any(CreateBookingRequest.class), any(BookingEntity.class));
//         verify(bookingRepository, never()).save(any(BookingEntity.class));
//     }

//     @Test
//     void delete_existingId_deletesById() {
//         when(bookingRepository.existsById(3L)).thenReturn(true);

//         bookingService.delete(3L);

//         verify(bookingRepository).deleteById(3L);
//     }

//     @Test
//     void delete_unknownId_throwsEntityNotFoundAndDoesNotDelete() {
//         when(bookingRepository.existsById(404L)).thenReturn(false);

//         EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
//                 () -> bookingService.delete(404L));

//         assertTrue(ex.getMessage().contains("404"));
//         verify(bookingRepository, never()).deleteById(anyLong());
//     }

//     private static CreateBookingRequest request(String title) {
//         CreateBookingRequest request = new CreateBookingRequest();
//         request.setTitle(title);
//         request.setCustomerEmail("client@example.com");
//         request.setStartTime(START);
//         request.setEndTime(END);
//         return request;
//     }

//     private static BookingEntity entity(Long id, String title) {
//         return BookingEntity.builder()
//                 .id(id)
//                 .title(title)
//                 .customerEmail("client@example.com")
//                 .startTime(START)
//                 .endTime(END)
//                 .status(BookingStatus.PENDING)
//                 .build();
//     }

//     private static BookingDto dto(Long id, String title) {
//         BookingDto dto = new BookingDto();
//         dto.setId(id);
//         dto.setTitle(title);
//         dto.setCustomerEmail("client@example.com");
//         dto.setStartTime(START);
//         dto.setEndTime(END);
//         dto.setStatus(BookingStatus.PENDING);
//         return dto;
//     }
// }
