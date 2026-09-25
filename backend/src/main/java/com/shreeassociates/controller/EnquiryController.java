package com.shreeassociates.controller;

import com.shreeassociates.dto.EnquiryRequest;
import com.shreeassociates.dto.EnquiryResponse;
import com.shreeassociates.entity.Enquiry;
import com.shreeassociates.service.EnquiryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    private static final Logger log = LoggerFactory.getLogger(EnquiryController.class);

    private final EnquiryService enquiryService;

    public EnquiryController(EnquiryService enquiryService) {
        this.enquiryService = enquiryService;
    }

    @PostMapping
    public ResponseEntity<EnquiryResponse> submitEnquiry(@Valid @RequestBody EnquiryRequest request,
                                                           HttpServletRequest httpRequest) {

        // Honeypot: a hidden field real users never fill. If it has content, silently
        // report success without saving or notifying, so bots see no signal to adapt to.
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            log.info("Honeypot triggered - likely bot submission, ignoring silently");
            return ResponseEntity.ok(EnquiryResponse.ok("Your enquiry has been submitted successfully.", null));
        }

        String clientIp = resolveClientIp(httpRequest);
        Enquiry saved = enquiryService.submitEnquiry(request, clientIp);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EnquiryResponse.ok("Your enquiry has been submitted successfully.", saved.getId()));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
