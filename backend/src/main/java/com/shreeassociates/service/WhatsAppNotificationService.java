package com.shreeassociates.service;

import com.shreeassociates.config.WhatsAppProperties;
import com.shreeassociates.entity.Enquiry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Sends "New Website Enquiry" notifications to the Shree Associates WhatsApp
 * number using the official Meta WhatsApp Business Cloud API (Graph API).
 *
 * Kept as its own service (not mixed into EnquiryService) so the integration
 * can be swapped, mocked in tests, or pointed at a different provider later
 * without touching enquiry-storage logic.
 *
 * IMPORTANT - Meta's 24-hour session rule:
 * WhatsApp only allows free-form text messages to a number that has messaged
 * your business within the last 24 hours. A website visitor's enquiry is a
 * *business-initiated* message to the Shree Associates office number, which
 * will usually be outside that session window, so Meta requires an
 * **approved message template** for this notification (useTemplate=true,
 * the default). See README.md -> "WhatsApp Business Cloud API Setup" for how
 * to create and approve the template in Meta Business Manager.
 */
@Service
public class WhatsAppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationService.class);
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a");

    private final RestTemplate restTemplate;
    private final WhatsAppProperties properties;

    public WhatsAppNotificationService(RestTemplate whatsAppRestTemplate, WhatsAppProperties properties) {
        this.restTemplate = whatsAppRestTemplate;
        this.properties = properties;
    }

    public static final class SendResult {
        public final boolean success;
        public final String messageId;
        public final String errorMessage;

        private SendResult(boolean success, String messageId, String errorMessage) {
            this.success = success;
            this.messageId = messageId;
            this.errorMessage = errorMessage;
        }

        public static SendResult success(String messageId) {
            return new SendResult(true, messageId, null);
        }

        public static SendResult failure(String errorMessage) {
            return new SendResult(false, null, errorMessage);
        }
    }

    /**
     * Sends the enquiry notification. Never throws - all failures (network,
     * timeout, auth, Meta API errors) are caught and returned as a failed
     * SendResult so the enquiry can still be saved and marked FAILED.
     */
    public SendResult sendEnquiryNotification(Enquiry enquiry) {
        if (!properties.isConfigured()) {
            log.warn("WhatsApp credentials are not configured (whatsapp.access-token / phone-number-id / recipient-number). " +
                    "Skipping WhatsApp send for enquiry id={}", enquiry.getId());
            return SendResult.failure("WhatsApp integration is not configured");
        }

        String url = String.format(
                "https://graph.facebook.com/%s/%s/messages",
                properties.getApiVersion(),
                properties.getPhoneNumberId());

        Map<String, Object> body = properties.isUseTemplate()
                ? buildTemplatePayload(enquiry)
                : buildTextPayload(enquiry);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getAccessToken());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            String messageId = extractMessageId(response);
            log.info("WhatsApp notification sent for enquiry id={} messageId={}", enquiry.getId(), messageId);
            return SendResult.success(messageId);

        } catch (HttpClientErrorException | HttpServerErrorException httpEx) {
            // Meta returned a structured error (invalid token, unapproved template, bad number, etc.)
            log.error("WhatsApp API returned an error for enquiry id={}: {} - {}",
                    enquiry.getId(), httpEx.getStatusCode(), httpEx.getResponseBodyAsString());
            return SendResult.failure(summarizeApiError(httpEx));

        } catch (ResourceAccessException timeoutEx) {
            log.error("WhatsApp API network/timeout error for enquiry id={}: {}", enquiry.getId(), timeoutEx.getMessage());
            return SendResult.failure("Network timeout while contacting WhatsApp API");

        } catch (Exception ex) {
            log.error("Unexpected error sending WhatsApp notification for enquiry id={}", enquiry.getId(), ex);
            return SendResult.failure("Unexpected error while sending WhatsApp notification");
        }
    }

    /**
     * Template-based payload (recommended / usually required).
     *
     * Expected Meta template, created in Meta Business Manager > WhatsApp Manager > Message Templates:
     *   Name:     website_enquiry_notification   (configurable via whatsapp.template-name)
     *   Category: UTILITY
     *   Language: en / en_US               (configurable via whatsapp.template-language)
     *   Body:     "New website enquiry.\nName: {{1}}\nContact: {{2}}\nEmail: {{3}}\nService: {{4}}\nMessage: {{5}}\nReceived: {{6}}"
     *
     * The six body variables are sent in order below: name, phone, email, service, message, received-at.
     * If your approved template uses different variables/order, adjust buildTemplatePayload() to match.
     */
    private Map<String, Object> buildTemplatePayload(Enquiry enquiry) {
        List<Map<String, Object>> parameters = List.of(
                textParam(enquiry.getName()),
                textParam(enquiry.getPhone()),
                textParam(enquiry.getEmail()),
                textParam(displayService(enquiry)),
                textParam(enquiry.getMessage()),
                textParam(enquiry.getCreatedAt().format(DISPLAY_FORMAT))
        );

        Map<String, Object> component = Map.of(
                "type", "body",
                "parameters", parameters
        );

        Map<String, Object> template = Map.of(
                "name", properties.getTemplateName(),
                "language", Map.of("code", properties.getTemplateLanguage()),
                "components", List.of(component)
        );

        return Map.of(
                "messaging_product", "whatsapp",
                "to", properties.getRecipientNumber(),
                "type", "template",
                "template", template
        );
    }

    /**
     * Free-form text payload. Only works if the recipient number has messaged
     * the business WhatsApp number within the last 24 hours (Meta's session
     * rule) - kept available for whatsapp.use-template=false during local
     * testing with a sandbox/test number.
     */
    private Map<String, Object> buildTextPayload(Enquiry enquiry) {
        String text = String.format(
                "🔔 *New Website Enquiry*%n%n" +
                        "*Name:* %s%n" +
                        "*Contact:* %s%n" +
                        "*Email:* %s%n" +
                        "*Service:* %s%n" +
                        "*Message:* %s%n%n" +
                        "*Received:* %s",
                enquiry.getName(),
                enquiry.getPhone(),
                enquiry.getEmail(),
                displayService(enquiry),
                enquiry.getMessage(),
                enquiry.getCreatedAt().format(DISPLAY_FORMAT)
        );

        return Map.of(
                "messaging_product", "whatsapp",
                "to", properties.getRecipientNumber(),
                "type", "text",
                "text", Map.of("body", text, "preview_url", false)
        );
    }

    private String displayService(Enquiry enquiry) {
        if (enquiry.getService() != null && !enquiry.getService().isBlank()) {
            return enquiry.getService();
        }
        if (enquiry.getSubject() != null && !enquiry.getSubject().isBlank()) {
            return enquiry.getSubject();
        }
        return "Not specified";
    }

    private Map<String, Object> textParam(String value) {
        return Map.of("type", "text", "text", value == null ? "" : value);
    }

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> response) {
        try {
            List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
            if (messages != null && !messages.isEmpty()) {
                Object id = messages.get(0).get("id");
                return id != null ? id.toString() : null;
            }
        } catch (Exception ignored) {
            // response shape didn't match what we expected; not fatal
        }
        return null;
    }

    private String summarizeApiError(org.springframework.web.client.RestClientResponseException httpEx) {
        if (httpEx.getStatusCode() == HttpStatus.UNAUTHORIZED || httpEx.getStatusCode() == HttpStatus.FORBIDDEN) {
            return "Invalid or expired WhatsApp API credentials";
        }
        return "WhatsApp API rejected the request (HTTP " + httpEx.getStatusCode().value() + ")";
    }
}
