package com.llm.chats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
public class PromptController {

    private static final Logger log = LoggerFactory.getLogger(PromptController.class);
    private final ChatClient chatClient;

    @Value("classpath:/prompt-templates/coding-assistant.st")
    private Resource systemText;

    public PromptController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
}
