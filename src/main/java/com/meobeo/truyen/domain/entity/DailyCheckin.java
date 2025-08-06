package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "daily_checkins")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "user")
public class DailyCheckin {

    @EmbeddedId
    private DailyCheckinId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
}