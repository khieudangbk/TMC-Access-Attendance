package com.tmc.gatestation.entity;

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
@Table(name = "factories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class FactoryEntity {
    @Id
    @Column(name = "factory_id")
    private UUID factoryId;

    @Column(name = "factory_code", length = 50)
    private String factoryCode;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "active")
    private boolean active;
}
