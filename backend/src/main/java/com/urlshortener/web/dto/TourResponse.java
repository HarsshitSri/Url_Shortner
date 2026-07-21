package com.urlshortener.web.dto;

import java.util.List;

public record TourResponse(
        String title,
        List<TourStep> steps,
        boolean fromAi,
        String notice
) {
    public record TourStep(String heading, String body) {
    }
}
