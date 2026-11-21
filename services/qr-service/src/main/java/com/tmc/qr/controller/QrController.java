package com.tmc.qr.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tmc.qr.dto.request.IssueRequestDTO;
import com.tmc.qr.dto.response.IssuedQrDTO;
import com.tmc.qr.service.QrService;

@RestController
@RequestMapping("/me/qr")
public class QrController {

    private final QrService qrService;

    public QrController(QrService qrService) {
        this.qrService = qrService;
    }

    @PostMapping
    IssuedQrDTO issue(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody IssueRequestDTO request) {
        return qrService.issue(employeeId(jwt), request.factoryId());
    }

    @DeleteMapping
    void revoke(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody IssueRequestDTO request) {
        qrService.revoke(employeeId(jwt), request.factoryId());
    }

    private UUID employeeId(Jwt jwt) {
        try {
            UUID employeeId = UUID.fromString(jwt.getSubject());
            if (!employeeId.toString().equals(jwt.getClaimAsString("employee_id"))) {
                throw new IllegalArgumentException();
            }
            return employeeId;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EMPLOYEE_IDENTITY_REQUIRED");
        }
    }
}
