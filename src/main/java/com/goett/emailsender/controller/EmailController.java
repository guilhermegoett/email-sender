package com.goett.emailsender.controller;

import com.goett.emailsender.dto.SendEmailRequest;
import com.goett.emailsender.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(
            @RequestBody SendEmailRequest request) {

        try {

            emailService.sendEmail(
                    request.to(),
                    request.subject(),
                    request.cargo());

            return ResponseEntity.ok(
                    "E-mail enviado com sucesso.");

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Erro ao enviar e-mail: "
                                    + e.getMessage());
        }
    }

}
