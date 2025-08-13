package com.meobeo.truyen.domain.response.giftcode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GiftCodeListResponse {

    private List<GiftCodeResponse> giftCodes;
    private int totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
