package com.meobeo.truyen.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Embeddable
@Data
@EqualsAndHashCode
public class ReadingHistoryId implements Serializable {

    private Long userId;
    private Long chapterId;
}