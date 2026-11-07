package com.course.app.aop;

import com.course.app.booking.BookingDto;
import com.course.app.booking.BookingEntity;
import com.course.app.booking.BookingMapper;
import com.course.app.booking.BookingRepository;
import com.course.app.booking.BookingService;
import org.aspectj.lang.annotation.Around;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.support.AopUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AopWiringAndSelfInvocationTest {

    private static final String UNMASKED = "john.smith@example.com";
    private static final String MASKED = "j" + "*".repeat(9) + "@example.com";

    private static <T> T proxy(T target, Object aspect) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    @Test
    void customAnnotations_haveRuntimeRetentionAndMethodTarget() {
        for (Class<?> a : List.of(RateLimited.class, MaskEmail.class, Audited.class)) {
            assertEquals(RetentionPolicy.RUNTIME, a.getAnnotation(Retention.class).value(), a.getSimpleName());
            assertEquals(List.of(ElementType.METHOD), List.of(a.getAnnotation(Target.class).value()), a.getSimpleName());
        }
    }

    @Test
    void aspects_usePointcutBasedOnTheirAnnotation() throws Exception {
        assertPointcutMentions(RateLimitAspect.class, "limit", "@annotation(rateLimited)");
        assertPointcutMentions(MaskEmailAspect.class, "mask", "@annotation(com.course.app.aop.MaskEmail)");
        assertPointcutMentions(AuditAspect.class, "audit", "@annotation(audited)");
    }

    private static void assertPointcutMentions(Class<?> aspect, String method, String expected) {
        Method m = java.util.Arrays.stream(aspect.getDeclaredMethods())
                .filter(x -> x.getName().equals(method)).findFirst().orElseThrow();
        assertEquals(expected, m.getAnnotation(Around.class).value());
    }

    @Test
    void annotationsAreActuallyPresentOnRealBookingServiceMethods() throws Exception {
        assertTrue(BookingService.class.getMethod("findAll").isAnnotationPresent(MaskEmail.class));
        assertTrue(BookingService.class.getMethod("delete", Long.class).isAnnotationPresent(Audited.class));
        assertTrue(BookingService.class.getMethod("update", Long.class,
                com.course.app.booking.CreateBookingRequest.class).isAnnotationPresent(Audited.class));
        assertTrue(BookingService.class.getMethod("create",
                com.course.app.booking.CreateBookingRequest.class).isAnnotationPresent(RateLimited.class));
    }

    @Test
    void externalCall_throughProxy_aspectRuns() {
        SelfInvocationDemo proxy = proxy(new SelfInvocationDemo(), new MaskEmailAspect());

        assertTrue(AopUtils.isAopProxy(proxy));
        assertEquals(MASKED, proxy.sampleBooking().getCustomerEmail());
    }

    @Test
    void externalCall_onRealBookingService_masksEmailsOfReturnedBookings() {
        BookingRepository repo = mock(BookingRepository.class);
        BookingMapper mapper = mock(BookingMapper.class);
        BookingEntity entity = new BookingEntity();
        BookingDto dto = new BookingDto();
        dto.setCustomerEmail(UNMASKED);
        when(repo.findAll()).thenReturn(List.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);
        BookingService service = proxy(new BookingService(repo, mapper), new MaskEmailAspect());

        List<BookingDto> result = service.findAll();

        assertEquals(MASKED, result.get(0).getCustomerEmail());
    }

    @Test
    void selfInvocation_viaThis_aspectIsSilentlySkipped() {
        SelfInvocationDemo proxy = proxy(new SelfInvocationDemo(), new MaskEmailAspect());

        BookingDto viaThis = proxy.sampleBookingViaThis();

        assertEquals(UNMASKED, viaThis.getCustomerEmail());
    }

    @Test
    void sameAnnotatedMethod_twoCallPaths_giveDifferentResults() {
        SelfInvocationDemo proxy = proxy(new SelfInvocationDemo(), new MaskEmailAspect());

        String external = proxy.sampleBooking().getCustomerEmail();
        String selfInvoked = proxy.sampleBookingViaThis().getCustomerEmail();

        assertEquals(MASKED, external);
        assertEquals(UNMASKED, selfInvoked);
    }
}
