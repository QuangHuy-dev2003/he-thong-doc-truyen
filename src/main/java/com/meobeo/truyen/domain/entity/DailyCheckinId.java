package com.meobeo.truyen.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Data
@EqualsAndHashCode
public class DailyCheckinId implements Serializable {

    private Long userId;
    private LocalDate checkinDate;
}