package evo.developers.com.cashcare.component;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenRouterClient {

    private static final String CHAT_COMPLETIONS_URL = "https://openrouter.ai/api/v1/chat/completions";

    private final RestTemplate restTemplate;

    @Value("${openrouter.api-key}")
    private String apiKey;

    @Value("${openrouter.api-model}")
    private String apiModel;

    public OpenRouterClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String ask(String systemPrompt, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("HTTP-Referer", "https://cashcare.local");
        headers.set("X-Title", "CashCare");

        HttpEntity<Map<String, Object>> request = buildRequest(systemPrompt, prompt, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                CHAT_COMPLETIONS_URL,
                HttpMethod.POST,
                request,
                Map.class
        );

        Map body = response.getBody();
        if (body == null) {
            throw new RuntimeException("AI вернул пустой ответ");
        }

        var choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("AI не вернул варианты ответа");
        }

        var message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private @NonNull HttpEntity<Map<String, Object>> buildRequest(String systemPrompt, String prompt, HttpHeaders headers) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", apiModel);
        body.put("temperature", 0.2);
        body.put("max_tokens", 1500);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", prompt)
        ));

        return new HttpEntity<>(body, headers);
    }
}
