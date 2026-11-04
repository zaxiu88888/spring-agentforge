package com.agentforge.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    /** GET /health —— 探活端点（api-contract.md §7） */
    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
