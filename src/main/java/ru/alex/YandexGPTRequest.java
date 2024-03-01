package ru.alex;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class YandexGPTRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(YandexGPTRequest.class);

    public static void main(String[] args) {
        try {
            // Загрузка значений из файла config.properties
            Properties properties = new Properties();
            properties.load(YandexGPTRequest.class.getClassLoader().getResourceAsStream("config.properties"));

            String apiKey = properties.getProperty("api.key");
            String folderId = properties.getProperty("folder.id");

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                String apiUrl = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion";
                HttpPost request = new HttpPost(apiUrl);

                request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
                request.addHeader(HttpHeaders.AUTHORIZATION, "Api-Key " + apiKey);
                // request.addHeader("x-folder-id", folderId); в режиме чата не требуется

                //  Значение "user" для роли. Можно менять в зависимости от нужд. Роль system определяет характер беседы
                String role = "user";

                while (true) {
                    System.out.println("\nВведите текст сообщения (или 'exit' для выхода): ");
                    String text = new BufferedReader(new InputStreamReader(System.in)).readLine();

                    if (text.equalsIgnoreCase("exit")) {
                        break;
                    }

                    // Формирование JSON-тела запроса
                    String requestBody = String.format("{\"modelUri\":\"gpt://%s/yandexgpt-lite\",\"completionOptions\":{\"stream\":false,\"temperature\":0.6,\"maxTokens\":\"2000\"},\"messages\":[{\"role\":\"%s\",\"text\":\"%s\"}]}", folderId, role, text);

                    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

                    try (CloseableHttpResponse response = httpClient.execute(request)) {
                        HttpEntity entity = response.getEntity();

                        if (entity != null) {
                            try (BufferedReader responseReader = new BufferedReader(new InputStreamReader(entity.getContent()))) {
                                String line;
                                StringBuilder responseContent = new StringBuilder();

                                while ((line = responseReader.readLine()) != null) {
                                    responseContent.append(line);
                                }

                                // Обработка ответа
                                String jsonResponse = responseContent.toString();
                                String textResponse = extractTextFromJson(jsonResponse);
                                System.out.println("Ответ Алиски: " + textResponse);
                                System.out.println("\nОтвет модели: " + jsonResponse);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Произошла ошибка:", e);
        }
    }

    private static String extractTextFromJson(String jsonResponse) {
        // Простейший способ извлечения текста из JSON
        int startIndex = jsonResponse.indexOf("\"text\":\"") + 8;
        int endIndex = jsonResponse.indexOf("\"", startIndex);
        return jsonResponse.substring(startIndex, endIndex);
    }
}
