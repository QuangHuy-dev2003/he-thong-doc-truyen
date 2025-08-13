package com.meobeo.truyen.domain.response.story;

import com.meobeo.truyen.repository.StoryViewsDailyRepository;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class AnalyticsSeriesResponse {

    private List<DataPoint> points = new ArrayList<>();

    public static AnalyticsSeriesResponse from(List<? extends StoryViewsDailyRepository.DateViewsProjection> rows) {
        AnalyticsSeriesResponse resp = new AnalyticsSeriesResponse();
        for (var r : rows) {
            DataPoint p = new DataPoint();
            p.setDate(r.getPeriod());
            p.setViews(r.getViews());
            resp.points.add(p);
        }
        return resp;
    }

    @Data
    public static class DataPoint {
        private LocalDate date;
        private Long views;
    }
}
