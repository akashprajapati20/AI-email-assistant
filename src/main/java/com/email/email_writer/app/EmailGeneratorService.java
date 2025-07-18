package com.email.email_writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {
    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClient){
        this.webClient=webClient.build();
    }
    public String generateEmailReply(EmailRequest emailRequest){
        //Build the prompt
        String prompt =buildPrompt(emailRequest);
        //craft a request body
        Map<String,Object> requestBody =Map.of(
                "contents", new Object[]{
                        Map.of("parts",new Object[]{
                                Map.of("text",prompt)
                        })
                }
        );
        //do request and get response
        String response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("generativelanguage.googleapis.com")
                        .path("/v1beta/models/gemini-2.0-flash:generateContent")
                        .queryParam("key", geminiApiKey)
                        .build())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        //return extracted responseresponse

        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
      try{
          ObjectMapper mapper=new ObjectMapper();
          JsonNode rootNode=mapper.readTree(response);
          return rootNode.path("candidates")
                  .get(0)
                  .path("content")
                  .path("parts")
                  .get(0)
                  .path("text")
                  .asText();

      } catch (Exception e) {
          return e.getMessage();
      }
    }

    private String buildPrompt(EmailRequest emailRequest) {
    StringBuilder prompt =new StringBuilder();
    prompt.append("generate a professtional email reply for the following email content . do not give a subject line ");
    if(emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty()){
        prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");
    }
    prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());
    return prompt.toString();
    }

}
