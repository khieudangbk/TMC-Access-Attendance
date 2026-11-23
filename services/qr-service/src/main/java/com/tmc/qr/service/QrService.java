package com.tmc.qr.service;

import java.util.UUID;

import com.tmc.qr.dto.response.IssuedQrDTO;
import com.tmc.qr.dto.response.QrVerificationDTO;

public interface QrService {

    IssuedQrDTO issue(UUID employeeId, UUID factoryId);

    void revoke(UUID employeeId, UUID factoryId);

    QrVerificationDTO verify(String encodedToken, UUID expectedFactoryId);
}
