package com.shreeassociates.repository;

import com.shreeassociates.entity.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {

    /**
     * Used for duplicate-submission detection: same email + phone + message
     * received again within a short window is treated as an accidental
     * double-click rather than a genuine second enquiry.
     */
    Optional<Enquiry> findFirstByEmailIgnoreCaseAndPhoneAndMessageAndCreatedAtAfterOrderByCreatedAtDesc(
            String email, String phone, String message, LocalDateTime after);
}
