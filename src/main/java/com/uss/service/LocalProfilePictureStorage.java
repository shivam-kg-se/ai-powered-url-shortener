package com.uss.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalProfilePictureStorage implements ProfilePictureStorage {
    private final Path root;

    public LocalProfilePictureStorage(@Value("${app.storage.local-directory:./uploads/profile-pictures}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public String store(byte[] content, String extension) throws IOException {
        Files.createDirectories(root);
        String key = UUID.randomUUID() + "." + extension;
        Files.write(resolve(key), content);
        return key;
    }

    @Override
    public StoredPicture load(String storageKey) throws IOException {
        Path path = resolve(storageKey);
        String contentType = switch (extension(storageKey)) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
        return new StoredPicture(Files.readAllBytes(path), contentType);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || key.contains("/") || key.contains("\\")) {
            throw new IllegalArgumentException("Invalid profile picture storage key");
        }
        return path;
    }

    private String extension(String key) {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? "" : key.substring(dot + 1).toLowerCase();
    }
}
