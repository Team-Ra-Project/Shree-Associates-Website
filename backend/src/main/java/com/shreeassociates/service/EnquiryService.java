package com.shreeassociates.service;

import com.shreeassociates.dto.EnquiryRequest;
import com.shreeassociates.entity.Enquiry;
import com.shreeassociates.exception.DuplicateEnquiryException;
import com.shreeassociates.repository.EnquiryRepository;
import com.shreeassociates.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EnquiryService {

    private static final Logger log = LoggerFactory.getLogger(EnquiryService.class);

    /** Same email+phone+message combination within this window is treated as an accidental duplicate. */
    private static final int DUPLICATE_WINDOW_SECONDS = 60;

    private final EnquiryRepository enquiryRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;

    public EnquiryService(EnquiryRepository enquiryRepository,
                           WhatsAppNotificationService whatsAppNotificationService) {
        this.enquiryRepository = enquiryRepository;
        this.whatsAppNotificationService = whatsAppNotificationService;
    }

    /**
     * Validates (already done via @Valid on the controller), sanitizes,
     * persists, and attempts the WhatsApp notification for a new enquiry.
     * The enquiry is ALWAYS saved, even if the WhatsApp send fails.
     */
    @Transactional
    public Enquiry submitEnquiry(EnquiryRequest request, String clientIp) {
        Enquiry enquiry = new Enquiry();
        enquiry.setName(InputSanitizer.sanitize(request.getName()));
        enquiry.setPhone(InputSanitizer.sanitize(request.getPhone()));
        enquiry.setEmail(InputSanitizer.sanitize(request.getEmail()).toLowerCase());
        enquiry.setService(InputSanitizer.sanitize(request.getService()));
        enquiry.setSubject(InputSanitizer.sanitize(
                (request.getSubject() != null && !request.getSubject().isBlank())
                        ? request.getSubject()
                        : request.getService()));
        enquiry.setMessage(InputSanitizer.sanitize(request.getMessage()));
        enquiry.setClientIp(clientIp);
        enquiry.setCreatedAt(LocalDateTime.now());
        enquiry.setWhatsappStatus(Enquiry.WhatsAppStatus.PENDING);

        checkForDuplicate(enquiry);

        Enquiry saved = enquiryRepository.save(enquiry);
        log.info("Enquiry saved with id={}", saved.getId());

        sendWhatsAppNotification(saved);

        return saved;
    }

    private void checkForDuplicate(Enquiry candidate) {
        LocalDateTime windowStart = LocalDateTime.now().minusSeconds(DUPLICATE_WINDOW_SECONDS);
        Optional<Enquiry> existing = enquiryRepository
                .findFirstByEmailIgnoreCaseAndPhoneAndMessageAndCreatedAtAfterOrderByCreatedAtDesc(
                        candidate.getEmail(), candidate.getPhone(), candidate.getMessage(), windowStart);

        if (existing.isPresent()) {
            log.info("Duplicate enquiry submission detected for email={} within {}s window",
                    candidate.getEmail(), DUPLICATE_WINDOW_SECONDS);
            throw new DuplicateEnquiryException(
                    "It looks like you've already submitted this enquiry. Our team will be in touch shortly.");
        }
    }

    /**
     * Sends the WhatsApp notification and updates the enquiry's status
     * accordingly. Runs after the initial save so the enquiry record exists
     * either way (SENT or FAILED), per the "never lose the enquiry" requirement.
     */
    private void sendWhatsAppNotification(Enquiry enquiry) {
        WhatsAppNotificationService.SendResult result = whatsAppNotificationService.sendEnquiryNotification(enquiry);

        if (result.success) {
            enquiry.setWhatsappStatus(Enquiry.WhatsAppStatus.SENT);
            enquiry.setWhatsappMessageId(result.messageId);
        } else {
            enquiry.setWhatsappStatus(Enquiry.WhatsAppStatus.FAILED);
            log.warn("WhatsApp notification failed for enquiry id={}: {}", enquiry.getId(), result.errorMessage);
        }
        enquiryRepository.save(enquiry);
    }
}
