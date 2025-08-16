package com.meobeo.truyen.domain.response.chapter;

import lombok.Data;

@Data
public class ChapterLockStatusResponse {

    private Long chapterId;
    private Boolean isLocked;
    private Boolean isUnlockedByUser;
}
