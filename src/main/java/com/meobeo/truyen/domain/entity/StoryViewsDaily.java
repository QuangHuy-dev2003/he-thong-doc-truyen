package com.meobeo.truyen.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "story_views_daily")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "story")
public class StoryViewsDaily {

    @EmbeddedId
    private StoryViewsDailyId id;

    @Column(name = "views")
    private Integer views = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("storyId")
    @JoinColumn(name = "story_id")
    private Story story;
}