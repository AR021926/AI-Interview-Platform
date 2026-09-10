package com.interview.platform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/hello")
    public String hello() {
        return "AI Interview Platform Backend is working!";
    }
}