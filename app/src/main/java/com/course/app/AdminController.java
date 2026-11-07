package com.course.app;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class AdminController {

    @GetMapping
    public String reports() {
        return "Sensitive report data - USER only";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/more")
    public String moreReports() {
        return "Extremely sensitive report data - ADMIN only";
    }
}