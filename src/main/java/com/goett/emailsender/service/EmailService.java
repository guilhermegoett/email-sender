package com.goett.emailsender.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

@Service
public class EmailService {

    private final Gmail gmail;

    private final String templatePath;
    private final String cvPath;

    public EmailService(
            Gmail gmail,
            @Value("${email.template.path}") String templatePath,
            @Value("${email.cv.path}") String cvPath) {

        this.gmail = gmail;
        this.templatePath = templatePath;
        this.cvPath = cvPath;
    }

    public void sendEmail(
            String to,
            String subject,
            String cargo) throws Exception {

        String body = loadEmailTemplate(cargo);

        MimeMessage mimeMessage = createMimeMessage(
                to,
                subject,
                body);

        Message gmailMessage = createGmailMessage(mimeMessage);

        gmail.users()
                .messages()
                .send("me", gmailMessage)
                .execute();
    }

    private String loadEmailTemplate(String cargo)
            throws Exception {

        Path path = Paths.get(templatePath);

        if (!Files.exists(path)) {
            throw new IllegalStateException(
                    "Template de e-mail não encontrado: "
                            + path.toAbsolutePath());
        }

        String template = Files.readString(
                path,
                StandardCharsets.UTF_8);

        return template.replace(
                "{{cargo}}",
                escapeHtml(cargo));
    }

    private MimeMessage createMimeMessage(
            String to,
            String subject,
            String body) throws Exception {

        Properties properties = new Properties();

        Session session = Session.getInstance(properties);

        MimeMessage mimeMessage = new MimeMessage(session);

        mimeMessage.setFrom(
                new InternetAddress("me"));

        mimeMessage.setRecipient(
                RecipientType.TO,
                new InternetAddress(to));

        mimeMessage.setSubject(
                subject,
                StandardCharsets.UTF_8.name());

        MimeBodyPart htmlPart = createHtmlPart(body);

        MimeBodyPart attachmentPart = createCvAttachment();

        Multipart multipart = new MimeMultipart();

        multipart.addBodyPart(htmlPart);
        multipart.addBodyPart(attachmentPart);

        mimeMessage.setContent(multipart);

        return mimeMessage;
    }

    private MimeBodyPart createHtmlPart(String body)
            throws Exception {

        MimeBodyPart htmlPart = new MimeBodyPart();

        htmlPart.setContent(
                body,
                "text/html; charset=UTF-8");

        return htmlPart;
    }

    private MimeBodyPart createCvAttachment()
            throws Exception {

        Path path = Paths.get(cvPath);

        if (!Files.exists(path)) {
            throw new IllegalStateException(
                    "Currículo não encontrado: "
                            + path.toAbsolutePath());
        }

        MimeBodyPart attachmentPart = new MimeBodyPart();

        FileDataSource fileDataSource = new FileDataSource(path.toFile());

        attachmentPart.setDataHandler(
                new DataHandler(fileDataSource));

        attachmentPart.setFileName(
                path.getFileName().toString());

        return attachmentPart;
    }

    private Message createGmailMessage(
            MimeMessage mimeMessage) throws Exception {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        mimeMessage.writeTo(outputStream);

        String encodedEmail = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        outputStream.toByteArray());

        Message message = new Message();

        message.setRaw(encodedEmail);

        return message;
    }

    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
