package com.llm.chats;

import com.llm.dto.UserInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class PromptController {


    private static final Logger log = LoggerFactory.getLogger(PromptController.class);
    private final ChatClient chatClient;
    @Value("classpath:/prompt-templates/java-coding-assistant.st")
    private Resource systemTemplateMessage;

    @Value("classpath:/prompt-templates/coding-assistant.st")
    private Resource systemText;


    public PromptController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @PostMapping("/v1/prompts")
    public String prompts(@RequestBody UserInput userInput){
        log.info("userInput : {} ", userInput);
        var systemMessage = """
                You are a helpful assistant, who can answer java based questions.
                For any other questions, please respond with I don't know in a funny way!
                """;
        var sysMessage = new SystemMessage(systemTemplateMessage);
        var userMessage = new UserMessage(userInput.prompt());

        var promptMessage = new Prompt(List.of(sysMessage,
//                new UserMessage("Whats My name ?"),
//                new AssistantMessage("I dont know!"),
//                new UserMessage("My name is Dilip"),
                userMessage));

        var responseSpec = chatClient.prompt(promptMessage).call();
        return responseSpec.content();

    }


    @PostMapping("/v1/prompts/{language}")
    public String promptsByLanguage(
            @PathVariable String language,
            @RequestBody UserInput userInput
    ){
        log.info("userInput : {}, language : {} ", userInput, language);
        var systemPromptTemplate = new SystemPromptTemplate(systemText);
        var sysMessage = systemPromptTemplate.createMessage(Map.of("language", language));

        log.info("sysMessage : {} ", sysMessage);

        var userMessage = new UserMessage(userInput.prompt());

        var promptMessage = new Prompt(List.of(sysMessage,
                userMessage));

        var responseSpec = chatClient.prompt(promptMessage).call();
        return responseSpec.content();

    }


    @PostMapping("/v2/prompts/{language}")
    public Object promptsByLanguageV2(
            @PathVariable String language,
            @RequestBody UserInput userInput) {

        log.info("userInput message : {} ", userInput);

        var requestSpec = chatClient
                .prompt()
                .user(userInput.prompt())
                .system(promptSystemSpec ->
                        promptSystemSpec.text(systemText)
                                .param("language", language));

        log.info("requestSpec : {} ", requestSpec);

        var responseSpec = requestSpec.call();
//        return responseSpec.chatResponse();
        return responseSpec.content();

    }


}
