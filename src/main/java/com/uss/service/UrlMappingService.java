package com.uss.service;


import com.uss.bindings.ClickEventDTO;
import com.uss.bindings.UrlMappingDTO;
import com.uss.models.ClickEvent;
import com.uss.models.UrlMapping;
import com.uss.models.User;
import com.uss.repository.ClickEventRepo;
import com.uss.repository.UrlMappingRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UrlMappingService {
    private final UrlMappingRepo urlMappingRepo;
    private final ClickEventRepo clickEventRepo;

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


    public List<ClickEventDTO> getClickEventByDate(
            String shortUrl,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        UrlMapping urlMapping = urlMappingRepo.findByShortUrl(shortUrl);

        // URL database mein nahi mila
        if (urlMapping == null) {
            return List.of();
        }

        List<ClickEvent> clickEvents = clickEventRepo.findByUrlMappingAndClickDateBetween(
                        urlMapping,
                        startDate,
                        endDate
                );
        return clickEvents.stream()
                .collect(Collectors.groupingBy(
                        click -> click.getClickDate().toLocalDate(),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    ClickEventDTO dto = new ClickEventDTO();
                    dto.setClickDate(entry.getKey());
                    dto.setCount(entry.getValue());
                    return dto;
                })
                .toList();
    }


    public Map<LocalDate, Long>  getTotalClicksByUserAndDate(User user, LocalDate startDate, LocalDate endDate) {
        List<UrlMapping> urlMappings = urlMappingRepo.findByUser(user);
        List<ClickEvent> clickEvents = clickEventRepo.findByUrlMappingInAndClickDateBetween(urlMappings, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        return clickEvents.stream()
                .collect(Collectors.groupingBy(click -> click.getClickDate().toLocalDate(), Collectors.counting()));
    }

    public UrlMapping getOriginalUrl(String shortUrl) {
        UrlMapping urlMapping = urlMappingRepo.findByShortUrl(shortUrl);
        if(urlMapping != null){
            urlMapping.setClickCount(urlMapping.getClickCount()+1);
            urlMappingRepo.save(urlMapping);
            ClickEvent clickEvent = new ClickEvent();
            clickEvent.setClickDate(LocalDateTime.now());
            clickEvent.setUrlMapping(urlMapping);
            clickEventRepo.save(clickEvent);
        }
            return urlMapping;
    }
}
