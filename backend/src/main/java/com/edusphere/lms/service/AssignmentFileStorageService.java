package com.edusphere.lms.service;

import com.edusphere.lms.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class AssignmentFileStorageService {
    private final Path root;
    private final String storageProvider;
    private final String cloudinaryCloudName;
    private final String cloudinaryApiKey;
    private final String cloudinaryApiSecret;
    private final String cloudinaryFolder;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AssignmentFileStorageService(
            @Value("${app.storage-root:storage}") String storageRoot,
            @Value("${app.storage.provider:local}") String storageProvider,
            @Value("${app.storage.cloudinary.cloud-name:}") String cloudinaryCloudName,
            @Value("${app.storage.cloudinary.api-key:}") String cloudinaryApiKey,
            @Value("${app.storage.cloudinary.api-secret:}") String cloudinaryApiSecret,
            @Value("${app.storage.cloudinary.folder:edusphere-lms}") String cloudinaryFolder,
            ObjectMapper objectMapper
    ) {
        this.root = Path.of(storageRoot).toAbsolutePath().normalize();
        this.storageProvider = storageProvider;
        this.cloudinaryCloudName = cloudinaryCloudName;
        this.cloudinaryApiKey = cloudinaryApiKey;
        this.cloudinaryApiSecret = cloudinaryApiSecret;
        this.cloudinaryFolder = cloudinaryFolder;
        this.objectMapper = objectMapper;
    }

    public String storeReferenceFile(MultipartFile file) {
        return store(file, "assignments/reference");
    }

    public String storeSubmissionFile(MultipartFile file) {
        return store(file, "assignments/submissions");
    }

    public Path resolve(String relativePath) {
        return root.resolve(relativePath).normalize();
    }

    public boolean isRemote(String storedName) {
        return storedName != null && (storedName.startsWith("http://") || storedName.startsWith("https://"));
    }

    public URI remoteUri(String storedName) {
        return URI.create(storedName);
    }

    private String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (useCloudinary()) {
            return storeInCloudinary(file, folder);
        }
        return storeLocally(file, folder);
    }

    private String storeLocally(MultipartFile file, String folder) {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename());
        String safeName = UUID.randomUUID() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dir = root.resolve(folder);
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(safeName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store uploaded file");
        }
        return folder + "/" + safeName;
    }

    private String storeInCloudinary(MultipartFile file, String folder) {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename());
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String publicId = UUID.randomUUID() + "-" + safeName;
        long timestamp = Instant.now().getEpochSecond();
        String uploadFolder = cloudinaryFolder + "/" + folder;

        Map<String, String> signatureParams = new TreeMap<>();
        signatureParams.put("folder", uploadFolder);
        signatureParams.put("public_id", publicId);
        signatureParams.put("timestamp", String.valueOf(timestamp));

        try {
            String boundary = "----EduSphereBoundary" + UUID.randomUUID();
            byte[] body = buildMultipartBody(boundary, file, uploadFolder, publicId, timestamp);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudinaryCloudName + "/auto/upload"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Cloudinary upload failed: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String secureUrl = json.path("secure_url").asText();
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Cloudinary upload returned no secure URL");
            }
            return secureUrl;
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Cloudinary upload was interrupted");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Unable to upload file to Cloudinary");
        }
    }

    private byte[] buildMultipartBody(String boundary, MultipartFile file, String folder, String publicId, long timestamp) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeFormField(output, boundary, "folder", folder);
        writeFormField(output, boundary, "public_id", publicId);
        writeFormField(output, boundary, "timestamp", String.valueOf(timestamp));
        writeFormField(output, boundary, "api_key", cloudinaryApiKey);
        writeFormField(output, boundary, "signature", sign(folder, publicId, timestamp));

        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        String fileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename());
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + (file.getContentType() == null ? "application/octet-stream" : file.getContentType()) + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(file.getBytes());
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private void writeFormField(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String folder, String publicId, long timestamp) {
        String payload = "folder=" + folder + "&public_id=" + publicId + "&timestamp=" + timestamp + cloudinaryApiSecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign Cloudinary upload request");
        }
    }

    private boolean useCloudinary() {
        return "cloudinary".equalsIgnoreCase(storageProvider)
                && !cloudinaryCloudName.isBlank()
                && !cloudinaryApiKey.isBlank()
                && !cloudinaryApiSecret.isBlank();
    }
}
