package com.goett.emailsender.dto;

public record SendEmailRequest(
        String to,
        String subject,
        String cargo) {
}
