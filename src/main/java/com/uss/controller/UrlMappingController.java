package com.uss.controller;

import com.uss.bindings.ClickEventDTO;
import com.uss.bindings.UrlMappingDTO;
import com.uss.models.User;
import com.uss.service.UrlMappingService;
import com.uss.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/urls")
@AllArgsConstructor
public class UrlMappingController {
    private final UrlMappingService urlMappingService;
    private final UserService userService;

//    {"originalUrl":"www.shivam.com"}
    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UrlMappingDTO> createShortUrl(@RequestBody Map<String,String> request,
                                                        Principal principal){
        String originalUrl = request.get("originalUrl");
        User user = userService.findByUsername(principal.getName());
        UrlMappingDTO urlMappingDTO = urlMappingService.createShortUrl(originalUrl,user);
        return ResponseEntity.ok(urlMappingDTO);
    }
    @GetMapping("/myurls")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UrlMappingDTO>> getUserUrls(Principal principal){
        User user = userService.findByUsername(principal.getName());
        List<UrlMappingDTO> urlMappings = urlMappingService.getUserUrls(user);
        return ResponseEntity.ok(urlMappings);
    }

    @GetMapping("/analytics/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClickEventDTO>> getUrlAnalytics(@PathVariable String shortUrl
                                                         , @RequestParam("startDate") String StartDate
                                                         , @RequestParam("endDate") String EndDate){
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime startDate = LocalDateTime.parse(StartDate, formatter);
        LocalDateTime endDate = LocalDateTime.parse(EndDate, formatter);
        List<ClickEventDTO> clickEventDTO = urlMappingService.getClickEventByDate(shortUrl,startDate,endDate);
        return ResponseEntity.ok(clickEventDTO);
    }
    @GetMapping("/totalClicks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<LocalDate, Long>> getTotalClicksByDate(@RequestParam("startDate") String StartDate
                                                              , @RequestParam("endDate") String EndDate
                                                              , Principal principal){
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        User user = userService.findByUsername(principal.getName());
        LocalDate startDate = LocalDate.parse(StartDate, formatter);
        LocalDate endDate = LocalDate.parse(EndDate, formatter);
        Map<LocalDate, Long> totalClicks = urlMappingService.getTotalClicksByUserAndDate(user,startDate, endDate);
        return ResponseEntity.ok(totalClicks);
    }

}
