package com.urlshortener.tour;

import static org.assertj.core.api.Assertions.assertThat;

import com.urlshortener.web.dto.TourResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;

class TourServiceTest {

    @Test
    void getTour_returnsCannedTourWhenChatModelMissing() {
        ObjectProvider<ChatModel> emptyProvider = new ObjectProvider<>() {
            @Override
            public ChatModel getObject(Object... args) {
                return null;
            }

            @Override
            public ChatModel getIfAvailable() {
                return null;
            }

            @Override
            public ChatModel getIfUnique() {
                return null;
            }

            @Override
            public ChatModel getObject() {
                return null;
            }
        };

        TourService tourService = new TourService(emptyProvider);
        TourResponse tour = tourService.getTour();

        assertThat(tour.fromAi()).isFalse();
        assertThat(tour.steps()).hasSize(4);
        assertThat(tour.notice()).contains("AI tour is temporarily unavailable");
    }
}
