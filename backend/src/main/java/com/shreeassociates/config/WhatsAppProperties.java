package com.shreeassociates.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * All values are sourced from environment variables (or application.properties
 * placeholders backed by env vars) - never hard-coded, never committed to git.
 * See application.properties for the ${WHATSAPP_...} placeholders.
 */
@Component
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProperties {

    /** Permanent or system-user access token for the WhatsApp Business Cloud API. */
    private String accessToken;

    /** The "Phone Number ID" of the sender number, from Meta's App Dashboard (NOT the phone number itself). */
    private String phoneNumberId;

    /** The Shree Associates WhatsApp number that should RECEIVE the notification, in E.164 format e.g. 9198XXXXXXXX. */
    private String recipientNumber;

    /** Graph API version, e.g. v20.0 */
    private String apiVersion;

    /** Approved template name used to send the notification (see README for setup). */
    private String templateName;

    /** Language code the template was approved in, e.g. en or en_US. */
    private String templateLanguage;

    /** true = use an approved message template (required by Meta outside a 24h session). false = attempt a free-form text message. */
    private boolean useTemplate = true;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getRecipientNumber() {
        return recipientNumber;
    }

    public void setRecipientNumber(String recipientNumber) {
        this.recipientNumber = recipientNumber;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateLanguage() {
        return templateLanguage;
    }

    public void setTemplateLanguage(String templateLanguage) {
        this.templateLanguage = templateLanguage;
    }

    public boolean isUseTemplate() {
        return useTemplate;
    }

    public void setUseTemplate(boolean useTemplate) {
        this.useTemplate = useTemplate;
    }

    /** True once the minimum settings needed to call the Graph API are present. */
    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank()
                && phoneNumberId != null && !phoneNumberId.isBlank()
                && recipientNumber != null && !recipientNumber.isBlank();
    }
}
