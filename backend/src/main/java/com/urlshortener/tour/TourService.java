package com.urlshortener.tour;

import com.urlshortener.web.dto.TourResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TourService {

    private static final Logger log = LoggerFactory.getLogger(TourService.class);

    private static final String SYSTEM_PROMPT = """
            You write a short product tour for a URL shortener service called ShortLink.
            Return exactly 4 steps in this plain-text format (no markdown, no numbering extras):
            STEP: <short heading>
            BODY: <1-2 sentences>
            Cover: what the service does, how to create a short link, how redirects work, and soft AI safety warnings.
            Keep the tone clear and welcoming.
            """;

    private static final Pattern STEP_PATTERN = Pattern.compile(
            "STEP:\\s*(.+?)\\s*BODY:\\s*(.+?)(?=STEP:|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final ObjectProvider<ChatModel> chatModelProvider;

    public TourService(ObjectProvider<ChatModel> chatModelProvider) {
        this.chatModelProvider = chatModelProvider;
    }

    public TourResponse getTour() {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return cannedTour("AI tour is temporarily unavailable. Showing the standard walkthrough.");
        }

        try {
            String content = ChatClient.create(chatModel)
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user("Generate the tour now.")
                    .call()
                    .content();

            List<TourResponse.TourStep> steps = parseSteps(content);
            if (steps.size() < 3) {
                log.warn("AI tour response could not be parsed reliably: {}", content);
                return cannedTour("AI tour is temporarily unavailable. Showing the standard walkthrough.");
            }
            return new TourResponse("Welcome to ShortLink", steps, true, null);
        } catch (Exception ex) {
            log.error("Failed to generate AI tour", ex);
            return cannedTour("AI tour is temporarily unavailable. Showing the standard walkthrough.");
        }
    }

    private List<TourResponse.TourStep> parseSteps(String content) {
        List<TourResponse.TourStep> steps = new ArrayList<>();
        if (!StringUtils.hasText(content)) {
            return steps;
        }
        Matcher matcher = STEP_PATTERN.matcher(content);
        while (matcher.find()) {
            String heading = matcher.group(1).trim().replaceAll("\\s+", " ");
            String body = matcher.group(2).trim().replaceAll("\\s+", " ");
            if (StringUtils.hasText(heading) && StringUtils.hasText(body)) {
                steps.add(new TourResponse.TourStep(heading, body));
            }
        }
        return steps;
    }

    private TourResponse cannedTour(String notice) {
        List<TourResponse.TourStep> steps = List.of(
                new TourResponse.TourStep(
                        "What ShortLink does",
                        "Paste a long URL and get a compact short link you can share anywhere."),
                new TourResponse.TourStep(
                        "Create a short link",
                        "Use the form on this page. We accept http and https destinations up to 2048 characters."),
                new TourResponse.TourStep(
                        "How redirects work",
                        "Opening your short link sends visitors to the original destination with a 302 redirect."),
                new TourResponse.TourStep(
                        "AI safety notes",
                        "Gemini soft-checks destinations and may add warnings. Creation is never blocked — you decide whether to proceed."));
        return new TourResponse("Welcome to ShortLink", steps, false, notice);
    }
}
