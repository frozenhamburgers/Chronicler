package net.jelly.chronicler.ollama;

import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaClient {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String BASE_URL = "http://localhost:11434";
    private static final Gson GSON = new Gson();

    public static boolean isServerRunning() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/tags"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static String generate(String model, String prompt) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("prompt", prompt);
        body.addProperty("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(60)) // LLMs can be slow
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
        return json.get("response").getAsString();
    }

    public static String getServerError() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/tags"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return "HTTP " + response.statusCode() + ": " + response.body();
        } catch (Exception e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
