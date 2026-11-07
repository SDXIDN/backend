package com.course.app.aop;

import com.course.app.booking.BookingDto;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaskEmailAspectTest {

    private final MaskEmailAspect aspect = new MaskEmailAspect();
    private final ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

    private static BookingDto dto(String email) {
        BookingDto d = new BookingDto();
        d.setCustomerEmail(email);
        return d;
    }

    @Test
    void mask_singleDto_proceedsAndReplacesReturnedEmail() throws Throwable {
        BookingDto original = dto("john.smith@example.com");
        when(pjp.proceed()).thenReturn(original);

        Object result = aspect.mask(pjp);

        verify(pjp, times(1)).proceed();
        assertSame(original, result);
        assertEquals("j*********@example.com", ((BookingDto) result).getCustomerEmail());
    }

    @Test
    void mask_listOfDtos_masksEveryElement() throws Throwable {
        List<BookingDto> list = List.of(dto("anna@mail.com"), dto("bob@mail.com"));
        when(pjp.proceed()).thenReturn(list);

        aspect.mask(pjp);

        assertEquals("a***@mail.com", list.get(0).getCustomerEmail());
        assertEquals("b**@mail.com", list.get(1).getCustomerEmail());
    }

    @Test
    void mask_nullResult_returnsNull() throws Throwable {
        when(pjp.proceed()).thenReturn(null);

        assertNull(aspect.mask(pjp));
    }

    @Test
    void mask_resultOfOtherType_returnedUntouched() throws Throwable {
        when(pjp.proceed()).thenReturn("plain string");

        assertEquals("plain string", aspect.mask(pjp));
    }

    @Test
    void mask_dtoWithNullEmail_staysNull() throws Throwable {
        BookingDto d = dto(null);
        when(pjp.proceed()).thenReturn(d);

        aspect.mask(pjp);

        assertNull(d.getCustomerEmail());
    }

    @Test
    void mask_methodThrows_exceptionPropagates() throws Throwable {
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> aspect.mask(pjp));
    }

    @Test
    void maskEmail_variants() {
        assertEquals("*@x.com", MaskEmailAspect.maskEmail("a@x.com"));
        assertEquals("***", MaskEmailAspect.maskEmail("no-at-sign"));
        assertEquals("***", MaskEmailAspect.maskEmail("@x.com"));
        assertNull(MaskEmailAspect.maskEmail(null));
    }
}
