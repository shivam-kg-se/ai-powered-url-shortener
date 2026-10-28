package com.uss.service;

import java.io.IOException;

public interface ProfilePictureStorage {
    String store(byte[] content, String extension) throws IOException;
    StoredPicture load(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;

    record StoredPicture(byte[] content, String contentType) {
    }
}
