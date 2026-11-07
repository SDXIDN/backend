package com.course.app.aop;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AopDemoController {

    private final SelfInvocationDemo demo;

    public AopDemoController(SelfInvocationDemo demo) {
        this.demo = demo;
    }

    @GetMapping("/api/public/aop-demo")
    public Map<String, String> demo() {
        return Map.of(
                "viaProxy (external call, aspect runs)", demo.sampleBooking().getCustomerEmail(),
                "viaThis (self-invocation, aspect skipped)", demo.sampleBookingViaThis().getCustomerEmail());
    }
}
