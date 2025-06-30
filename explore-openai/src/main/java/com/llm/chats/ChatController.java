package com.llm.chats;


import com.llm.dto.UserInput;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@Validated
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .build();
    }


    @PostMapping("/v1/chats")
    public Object chat(@RequestBody @Valid UserInput userInput) {

        log.info("userInput message : {} ", userInput);
        var requestSpec = chatClient
                .prompt()
//                .advisors(new SimpleLoggerAdvisor())
                .system("You are helpful assistant!")
                .user(userInput.prompt());

        log.info("requestSpec : {} ", requestSpec);

        var responseSpec = requestSpec.call();

//        return responseSpec.chatResponse();
        return responseSpec.content();

    }

    @PostMapping("/v1/chats/stream")
    public Flux<String> chatWithStream(@RequestBody @Valid  UserInput userInput) {
        return chatClient.prompt()
                .user(userInput.prompt())
                .stream()
                .content()
                .doOnNext(s -> log.info("s : {}", s))
                .doOnComplete(() -> log.info("Data complete"))
                //.onErrorReturn("Error occurred while streaming data")
                .onErrorResume(throwable -> {
                    log.error("Error occurred: {}", throwable.getMessage());
//                    return Flux.just("Error occurred while streaming data : "+ throwable.getMessage());
                    return Flux.error(new RuntimeException("Error occurred while streaming data : "+ throwable.getMessage()));
                })
                ;

    }

    @PostMapping("/v2/chats")
    public Object chatV2(@RequestBody UserInput userInput) {

        log.info("userInput message : {} ", userInput);

        var systemMessage = """
                You are a helpful assistant, who can answer java based questions.
                For any other questions, please respond with I don't know in a funny way!
                """;

        var responseSpec = chatClient
                .prompt()
                .user(userInput.prompt())
                .system(systemMessage)
                .call();


//        return responseSpec.chatResponse();
        return responseSpec.content();

    }


}
