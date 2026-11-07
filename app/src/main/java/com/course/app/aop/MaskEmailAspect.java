package com.course.app.aop;

import com.course.app.booking.BookingDto;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Aspect
@Component
public class MaskEmailAspect {

    @Around("@annotation(com.course.app.aop.MaskEmail)")
    public Object mask(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        if (result instanceof BookingDto dto) {
            maskDto(dto);
        } else if (result instanceof Collection<?> items) {
            for (Object item : items) {
                if (item instanceof BookingDto dto) {
                    maskDto(dto);
                }
            }
        }
        return result;
    }

    private static void maskDto(BookingDto dto) {
        dto.setCustomerEmail(maskEmail(dto.getCustomerEmail()));
    }

    static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        if (at == 1) {
            return "*" + email.substring(at);
        }
        return email.charAt(0) + "*".repeat(at - 1) + email.substring(at);
    }
}
