package com.uss.service;


import com.uss.bindings.UrlMappingDTO;
import com.uss.models.UrlMapping;
import com.uss.models.User;
import com.uss.repository.UrlMappingRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UrlMappingService {
    private final UrlMappingRepo urlMappingRepo;

    public UrlMappingDTO createShortUrl(String originalUrl, User user) {
        String shortUrl= generateShortUrl();
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedDate(LocalDateTime.now());
        UrlMapping savedUrlMapping = urlMappingRepo.save(urlMapping);
        return convertToDTO(savedUrlMapping);
    }

    private UrlMappingDTO convertToDTO(UrlMapping urlMapping) {
        UrlMappingDTO dto = new UrlMappingDTO();
        dto.setId(urlMapping.getId());
        dto.setOriginalUrl(urlMapping.getOriginalUrl());
        dto.setShortUrl(urlMapping.getShortUrl());
        dto.setCreatedDate(urlMapping.getCreatedDate());
        return dto;
    }
    private String generateShortUrl() {
        Random random = new Random();
        StringBuilder shortUrl = new StringBuilder();
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < 8; i++) {
            shortUrl.append(characters.charAt(random.nextInt(characters.length())));
        }
        return shortUrl.toString(); // Return the generated short URL
    }

    public List<UrlMappingDTO> getUserUrls(User user) {
        List<UrlMapping> urlMappings = urlMappingRepo.findByUser(user);
        return urlMappings.stream().map(this::convertToDTO).toList();
    }
}
