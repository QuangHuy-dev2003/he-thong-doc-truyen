package com.meobeo.truyen.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Data
@EqualsAndHashCode
public class StoryViewsDailyId implements Serializable {

    private Long storyId;

    @Column(name = "view_date")
    private LocalDate viewDate;
}