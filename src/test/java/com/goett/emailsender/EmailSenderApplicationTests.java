package com.goett.emailsender;

import com.google.api.services.gmail.Gmail;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class EmailSenderApplicationTests {

    @MockitoBean
    private Gmail gmail;

    @Test
    void contextLoads() {
    }
}