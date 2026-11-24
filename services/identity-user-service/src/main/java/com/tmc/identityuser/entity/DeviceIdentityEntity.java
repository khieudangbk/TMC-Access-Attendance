package com.tmc.identityuser.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "device_identities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class DeviceIdentityEntity {
    @Id
    @Column(name = "client_id", length = 100)
    private String clientId;

    @Column(name = "station_id")
    private UUID stationId;

    @Column(name = "active")
    private boolean active;
}
