package com.meobeo.truyen.domain.entity;

import com.meobeo.truyen.domain.enums.BannerPosition;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "banners")
@Data
@EqualsAndHashCode(callSuper = false)
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "link_url")
    private String linkUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    private BannerPosition position = BannerPosition.TOP;
}