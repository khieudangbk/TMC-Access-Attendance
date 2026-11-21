package com.tmc.qr.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tmc.qr.config.QrSigningKeys;
import com.tmc.qr.dto.response.QrVerificationDTO;
import com.tmc.qr.dto.request.VerifyRequestDTO;
import com.tmc.qr.service.QrService;

@RestController
@RequestMapping("/internal/qr")
public class InternalQrController {

    private final QrService qrService;
    private final QrSigningKeys signingKeys;

    public InternalQrController(QrService qrService, QrSigningKeys signingKeys) {
        this.qrService = qrService;
        this.signingKeys = signingKeys;
    }

    @PostMapping("/verifications")
    QrVerificationDTO verify(@Valid @RequestBody VerifyRequestDTO request) {
        return qrService.verify(request.token(), request.factoryId());
    }

    @GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    String publicKeys() {
        return signingKeys.publicJwks();
    }
}
