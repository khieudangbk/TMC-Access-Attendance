package com.tmc.aacs.central.system.adapter.in.web;

import com.tmc.aacs.central.system.application.SystemStatusService;
import com.tmc.aacs.central.system.application.SystemStatusService.SystemStatus;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/status")
    public SystemStatus status() {
        return systemStatusService.current();
    }
}
