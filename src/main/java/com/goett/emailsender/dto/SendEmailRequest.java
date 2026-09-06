package com.goett.emailsender.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public record SendEmailRequest(
        @NotBlank(message = "Email não pode estar vazio")
        @Email(message = "Email inválido")
        String to,
        
        @NotBlank(message = "Assunto não pode estar vazio")
        String subject,
        
        @NotBlank(message = "Cargo não pode estar vazio")
        String cargo
) {}