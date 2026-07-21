package com.urlshortener.safety;

import com.urlshortener.domain.SafetyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Primary
@Profile("!test")
@ConditionalOnBean(ChatModel.class)
public class GeminiUrlSafetyService implements UrlSafetyService {

    private static final Logger log = LoggerFactory.getLogger(GeminiUrlSafetyService.class);

    public static final String AI_DOWN_WARNING =
            "Looks like our AI assistant is down. We can't verify this URL — proceed at your own risk.";

    public static final String HTTP_WARNING =
            "This destination uses HTTP (not HTTPS). Proceed at your own risk.";

    private static final String SYSTEM_PROMPT = """
            You are a URL safety classifier for a URL shortener service.
            Classify the destination URL as exactly one of: SAFE, SUSPICIOUS, UNSAFE.
            Rules:
            - SAFE: legitimate, non-malicious sites
            - SUSPICIOUS: phishing indicators, misleading domains, or unclear intent
            - UNSAFE: malware, phishing, scam, or clearly harmful content
            Respond with ONLY one word: SAFE, SUSPICIOUS, or UNSAFE.
            """;

    private final ChatClient chatClient;

    public GeminiUrlSafetyService(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    @Override
    public SafetyClassification classify(String originalUrl) {
        try {
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("Classify this URL: " + originalUrl)
                    .call()
                    .content();

            SafetyStatus status = parseStatus(response);
            if (status == null) {
                log.warn("Unrecognized safety classifier response: {}", response);
                return SafetyClassification.unavailable();
            }
            return new SafetyClassification(status, true);
        } catch (Exception ex) {
            log.error("URL safety classification failed for url={}", originalUrl, ex);
            return SafetyClassification.unavailable();
        }
    }

    private SafetyStatus parseStatus(String response) {
        if (!StringUtils.hasText(response)) {
            return null;
        }
        String normalized = response.trim().toUpperCase().replaceAll("[^A-Z]", "");
        return switch (normalized) {
            case "SAFE" -> SafetyStatus.SAFE;
            case "SUSPICIOUS" -> SafetyStatus.SUSPICIOUS;
            case "UNSAFE" -> SafetyStatus.UNSAFE;
            default -> null;
        };
    }
}
