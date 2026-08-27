package com.goett.emailsender.controller;

import com.goett.emailsender.dto.SendEmailRequest;
import com.goett.emailsender.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(
            @Valid @RequestBody SendEmailRequest request) {

        try {
            emailService.sendEmail(
                    request.to(),
                    request.subject(),
                    request.cargo());

            return ResponseEntity.status(HttpStatus.OK)
                    .body("E-mail enviado com sucesso.");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro de validação: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao enviar e-mail: " + e.getMessage());
        }
    }
}