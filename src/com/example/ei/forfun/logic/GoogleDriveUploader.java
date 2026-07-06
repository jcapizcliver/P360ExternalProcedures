package com.example.ei.forfun.logic;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleDriveUploader {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println("Uso:");
            System.err.println("java com.example.ei.forfun.logic.GoogleDriveUploader <clientId> <clientSecret> <refreshToken> <folderId> <filePath> <mimeType>");
            System.exit(1);
        }

        String clientId = args[0];
        String clientSecret = args[1];
        String refreshToken = args[2];
        String folderId = args[3];
        Path filePath = Path.of(args[4]);
        String mimeType = args[5];

        String accessToken = refreshAccessToken(clientId, clientSecret, refreshToken);
        String response = uploadFile(accessToken, folderId, filePath, mimeType);

        System.out.println("Respuesta Drive:");
        System.out.println(response);
    }

    public static String refreshAccessToken(String clientId, String clientSecret, String refreshToken)
            throws IOException, InterruptedException {

        String body = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&refresh_token=" + encode(refreshToken)
                + "&grant_type=refresh_token";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Error obteniendo access token. HTTP " + response.statusCode() + " body: " + response.body());
        }

        String accessToken = extractJsonString(response.body(), "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IOException("No se encontró access_token en la respuesta: " + response.body());
        }

        return accessToken;
    }

    public static String uploadFile(String accessToken, String folderId, Path filePath, String mimeType)
            throws IOException, InterruptedException {

        if (!Files.exists(filePath)) {
            throw new IOException("No existe el archivo: " + filePath);
        }

        String fileName = filePath.getFileName().toString();
        byte[] fileBytes = Files.readAllBytes(filePath);

        String boundary = "----gpt-" + UUID.randomUUID();
        String metadataJson = "{"
                + "\"name\":\"" + jsonEscape(fileName) + "\","
                + "\"parents\":[\"" + jsonEscape(folderId) + "\"]"
                + "}";

        byte[] bodyBytes = buildMultipartBody(boundary, metadataJson, mimeType, fileBytes);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DRIVE_UPLOAD_URL))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/related; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Error subiendo archivo a Drive. HTTP " + response.statusCode() + " body: " + response.body());
        }

        return response.body();
    }

    private static byte[] buildMultipartBody(String boundary, String metadataJson, String mimeType, byte[] fileBytes)
            throws IOException {

        String part1 = "--" + boundary + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                + metadataJson + "\r\n";

        String part2Headers = "--" + boundary + "\r\n"
                + "Content-Type: " + mimeType + "\r\n\r\n";

        String end = "\r\n--" + boundary + "--\r\n";

        byte[] part1Bytes = part1.getBytes(StandardCharsets.UTF_8);
        byte[] part2HeaderBytes = part2Headers.getBytes(StandardCharsets.UTF_8);
        byte[] endBytes = end.getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[part1Bytes.length + part2HeaderBytes.length + fileBytes.length + endBytes.length];

        int pos = 0;
        System.arraycopy(part1Bytes, 0, result, pos, part1Bytes.length);
        pos += part1Bytes.length;

        System.arraycopy(part2HeaderBytes, 0, result, pos, part2HeaderBytes.length);
        pos += part2HeaderBytes.length;

        System.arraycopy(fileBytes, 0, result, pos, fileBytes.length);
        pos += fileBytes.length;

        System.arraycopy(endBytes, 0, result, pos, endBytes.length);

        return result;
    }

    private static String extractJsonString(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}