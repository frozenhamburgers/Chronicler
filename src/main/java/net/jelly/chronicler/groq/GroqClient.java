package net.jelly.chronicler.groq;

import com.google.gson.*;
import net.jelly.chronicler.config.GroqConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocketFactory;
import java.nio.charset.StandardCharsets;

public class GroqClient {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String HOST = "api.groq.com";
    private static final int PORT = 443;
    private static final Gson GSON = new Gson();

    public static String generate(String prompt) throws Exception {
        String apiKey = GroqConfig.GROQ_API_KEY.get();
        String model = GroqConfig.GROQ_MODEL.get();

        // Build request body in OpenAI-compatible chat format
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("temperature", 0.7);
        body.addProperty("max_tokens", 1024);
        body.addProperty("stream", false);

        String bodyStr = body.toString();
        byte[] bodyBytes = bodyStr.getBytes(StandardCharsets.UTF_8);

        String rawRequest =
                "POST /openai/v1/chat/completions HTTP/1.0\r\n" +
                        "Host: api.groq.com\r\n" +
                        "Authorization: Bearer " + apiKey + "\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + bodyBytes.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" +
                        bodyStr;

        LOGGER.debug("[Groq] Sending request to {}", HOST);

        // Use SSL socket since Groq is HTTPS only
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (Socket socket = factory.createSocket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), 5000);
            socket.setSoTimeout(30000);

            // Send request
            OutputStream os = socket.getOutputStream();
            os.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            os.flush();

            // Read response
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );

            // Read status line
            String statusLine = reader.readLine();
            LOGGER.debug("[Groq] Status: {}", statusLine);

            // Read and skip headers
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                LOGGER.debug("[Groq] Header: {}", line);
            }

            // Read body
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String responseBody = sb.toString();
            LOGGER.debug("[Groq] Raw response: {}", responseBody);

            // Check for HTTP error
            if (statusLine != null && !statusLine.contains("200")) {
                throw new RuntimeException("Groq returned: " + statusLine + " — " + responseBody);
            }

            // Parse OpenAI-compatible response format
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);

            if (json.has("error")) {
                throw new RuntimeException("Groq error: " +
                        json.getAsJsonObject("error").get("message").getAsString());
            }

            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        }
    }
}
