package com.shreeassociates.dto;

/**
 * Uniform response shape returned to the frontend.
 * Never carries stack traces, tokens, or internal details.
 */
public class EnquiryResponse {

    private boolean success;
    private String message;
    private Long enquiryId;

    public EnquiryResponse() {
    }

    public EnquiryResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public EnquiryResponse(boolean success, String message, Long enquiryId) {
        this.success = success;
        this.message = message;
        this.enquiryId = enquiryId;
    }

    public static EnquiryResponse ok(String message, Long enquiryId) {
        return new EnquiryResponse(true, message, enquiryId);
    }

    public static EnquiryResponse error(String message) {
        return new EnquiryResponse(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getEnquiryId() {
        return enquiryId;
    }

    public void setEnquiryId(Long enquiryId) {
        this.enquiryId = enquiryId;
    }
}
