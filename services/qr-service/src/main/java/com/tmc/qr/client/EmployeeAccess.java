package com.tmc.qr.client;

import java.util.UUID;

public interface EmployeeAccess {

    boolean allowed(UUID employeeId, UUID factoryId);
}
