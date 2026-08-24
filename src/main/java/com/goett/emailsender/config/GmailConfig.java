package com.goett.emailsender.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@Profile("!test")
@Configuration
public class GmailConfig {

    private static final String APPLICATION_NAME = "Email Sender";

    private static final GsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();

    private static final String CREDENTIALS_FILE =
            "credentials/google-oauth-client.json";

    private static final String TOKENS_DIRECTORY =
            "tokens";

    private static final List<String> SCOPES =
            List.of(GmailScopes.GMAIL_SEND);

    @Bean
    public Gmail gmail() throws Exception {

        NetHttpTransport httpTransport =
                GoogleNetHttpTransport.newTrustedTransport();

        Credential credential = authorize(httpTransport);

        return new Gmail.Builder(
                httpTransport,
                JSON_FACTORY,
                credential
        )
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential authorize(NetHttpTransport httpTransport)
            throws Exception {

        File credentialsFile = new File(CREDENTIALS_FILE);

        if (!credentialsFile.exists()) {
            throw new IllegalStateException(
                    "Arquivo OAuth não encontrado: "
                            + credentialsFile.getAbsolutePath()
            );
        }

        try (InputStream inputStream =
                     new FileInputStream(credentialsFile)) {

            GoogleClientSecrets clientSecrets =
                    GoogleClientSecrets.load(
                            JSON_FACTORY,
                            new InputStreamReader(inputStream)
                    );

            GoogleAuthorizationCodeFlow flow =
                    new GoogleAuthorizationCodeFlow.Builder(
                            httpTransport,
                            JSON_FACTORY,
                            clientSecrets,
                            SCOPES
                    )
                            .setDataStoreFactory(
                                    new FileDataStoreFactory(
                                            new File(TOKENS_DIRECTORY)
                                    )
                            )
                            .setAccessType("offline")
                            .build();

            LocalServerReceiver receiver =
                    new LocalServerReceiver.Builder()
                            .setPort(8888)
                            .build();

            return new AuthorizationCodeInstalledApp(
                    flow,
                    receiver
            ).authorize("user");
        }
    }
}
