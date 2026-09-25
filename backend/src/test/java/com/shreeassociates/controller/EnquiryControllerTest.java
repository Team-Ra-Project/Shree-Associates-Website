package com.shreeassociates.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shreeassociates.entity.Enquiry;
import com.shreeassociates.repository.EnquiryRepository;
import com.shreeassociates.service.WhatsAppNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the scenarios called out in the project spec:
 * valid enquiry, invalid email, missing name, missing message,
 * WhatsApp API failure, and duplicate submission.
 *
 * WhatsAppNotificationService is mocked so tests never make real network calls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EnquiryRepository enquiryRepository;

    @MockBean
    private WhatsAppNotificationService whatsAppNotificationService;

    @BeforeEach
    void setUp() {
        enquiryRepository.deleteAll();
        when(whatsAppNotificationService.sendEnquiryNotification(any()))
                .thenReturn(WhatsAppNotificationService.SendResult.success("wamid.TEST123"));
    }

    private Map<String, String> validPayload() {
        Map<String, String> payload = new HashMap<>();
        payload.put("name", "Tanvi Thakur");
        payload.put("phone", "+919876543210");
        payload.put("email", "tanvi@example.com");
        payload.put("service", "Society Registration");
        payload.put("message", "I need assistance with society registration.");
        return payload;
    }

    @Test
    void validEnquiry_savesAndSendsWhatsAppAndReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.enquiryId").exists());

        Enquiry saved = enquiryRepository.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertEquals(Enquiry.WhatsAppStatus.SENT, saved.getWhatsappStatus());
    }

    @Test
    void invalidEmail_isRejected() throws Exception {
        Map<String, String> payload = validPayload();
        payload.put("email", "not-an-email");

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        org.junit.jupiter.api.Assertions.assertEquals(0, enquiryRepository.count());
    }

    @Test
    void missingName_isRejected() throws Exception {
        Map<String, String> payload = validPayload();
        payload.remove("name");

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(0, enquiryRepository.count());
    }

    @Test
    void missingMessage_isRejected() throws Exception {
        Map<String, String> payload = validPayload();
        payload.remove("message");

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(0, enquiryRepository.count());
    }

    @Test
    void whatsAppFailure_stillSavesEnquiryAndMarksFailed() throws Exception {
        when(whatsAppNotificationService.sendEnquiryNotification(any()))
                .thenReturn(WhatsAppNotificationService.SendResult.failure("Invalid or expired WhatsApp API credentials"));

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        Enquiry saved = enquiryRepository.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertEquals(Enquiry.WhatsAppStatus.FAILED, saved.getWhatsappStatus());
    }

    @Test
    void duplicateSubmission_isRejectedOnSecondAttempt() throws Exception {
        Map<String, String> payload = validPayload();

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/enquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        org.junit.jupiter.api.Assertions.assertEquals(1, enquiryRepository.count());
    }
}
