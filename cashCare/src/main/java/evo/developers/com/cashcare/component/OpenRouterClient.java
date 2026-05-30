package evo.developers.com.cashcare.component;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OpenRouterClient {

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

        HttpEntity<Map<String, Object>> request = getMapHttpEntity(systemPrompt, prompt, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://openrouter.ai/api/v1/chat/completions",
                HttpMethod.POST,
                request,
                Map.class
        );

        var choices = (List<Map<String, Object>>) response.getBody().get("choices");
        var message = (Map<String, Object>) choices.get(0).get("message");

        return (String) message.get("content");
    }

    private @NonNull HttpEntity<Map<String, Object>> getMapHttpEntity(String systemPrompt, String prompt, HttpHeaders headers) {
        Map<String, Object> body = Map.of(
                "model", apiModel,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                )
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);
        return request;
    }
}