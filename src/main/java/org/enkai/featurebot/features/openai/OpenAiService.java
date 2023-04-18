package org.enkai.featurebot.features.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.enkai.featurebot.features.openai.objects.*;
import org.enkai.featurebot.features.openai.objects.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class OpenAiService {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public Response send(ArrayList<ChatMessage> messages, int n) throws JsonProcessingException {
        Request request = new Request();
        request.temperature = Config.temperature;
        ChatMessage[] requestMessages = new ChatMessage[0];
        requestMessages = messages.toArray(requestMessages);
        request.messages = requestMessages;
        request.n = n;
        return send(request);
    }

    private Response send(Request request) throws JsonProcessingException {
        String response = "\"error\":{\"message\": \"Unexpected mapping error\"}";
        try {
            String body = mapper.writeValueAsString(request);
            response = doHttpRequest(body);
            return mapper.readValue(response, Response.class);
        } catch (JsonProcessingException exception) {
            System.out.println(exception.getMessage());
            ErrorMessage errorMessage = mapper.readValue(response, ErrorMessage.class);
            Response errorResponse = new Response();
            errorResponse.usage = new Usage(0);
            Choice[] choices = new Choice[1];
            choices[0] = new Choice(errorMessage.error.message);
            return errorResponse;
        }
    }

    private String doHttpRequest(String body) {
        System.out.println(body);
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .setHeader("Authorization", System.getenv("OpenAIToken"))
                    .setHeader("Content-Type", "application/json")
                    .uri(URI.create("https://api.openai.com/v1/chat/completions")).build();
            return client.send(httpRequest, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception exception) {
            return "\"error\":{\"message\": \"" + exception.getMessage() + "\"}";
        }
    }

    public static class Config {
        static public final int TOKEN_LIMIT = 9000000;
        static public String systemMessage = "";
        static public double temperature = 0.6;
    }

}

