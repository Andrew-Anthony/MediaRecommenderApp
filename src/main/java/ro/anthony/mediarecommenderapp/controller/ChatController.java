package ro.anthony.mediarecommenderapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.anthony.mediarecommenderapp.dto.chat.ChatRequest;
import ro.anthony.mediarecommenderapp.dto.chat.ChatResponse;
import ro.anthony.mediarecommenderapp.security.AuthorizationService;
import ro.anthony.mediarecommenderapp.service.ChatService;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        Long userId = authorizationService.getCurrentUserId();
        ChatResponse response = chatService.chat(userId, request.getMessages());
        return ResponseEntity.ok(response);
    }
}
