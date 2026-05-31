package ro.anthony.mediarecommenderapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.anthony.mediarecommenderapp.dto.chat.ChatFeedbackRequest;
import ro.anthony.mediarecommenderapp.dto.chat.ChatFeedbackResponse;
import ro.anthony.mediarecommenderapp.dto.chat.ChatFeedbackView;
import ro.anthony.mediarecommenderapp.dto.chat.ChatRequest;
import ro.anthony.mediarecommenderapp.dto.chat.ChatResponse;
import ro.anthony.mediarecommenderapp.model.FeedbackSignal;
import ro.anthony.mediarecommenderapp.security.AuthorizationService;
import ro.anthony.mediarecommenderapp.service.ChatFeedbackService;
import ro.anthony.mediarecommenderapp.service.ChatService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatFeedbackService chatFeedbackService;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        Long userId = authorizationService.getCurrentUserId();
        ChatResponse response = chatService.chat(userId, request.getMessages());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/feedback")
    public ResponseEntity<ChatFeedbackResponse> feedback(@Valid @RequestBody ChatFeedbackRequest request) {
        Long userId = authorizationService.getCurrentUserId();
        FeedbackSignal result = chatFeedbackService.record(userId, request.getTmdbId(), request.getSignal());
        return ResponseEntity.ok(new ChatFeedbackResponse(request.getTmdbId(), result));
    }

    @GetMapping("/feedback")
    public ResponseEntity<List<ChatFeedbackView>> getFeedback() {
        Long userId = authorizationService.getCurrentUserId();
        List<ChatFeedbackView> views = chatFeedbackService.getByUser(userId).stream()
                .map(fb -> new ChatFeedbackView(fb.getMovie().getTmdbId().intValue(), fb.getSignal()))
                .toList();
        return ResponseEntity.ok(views);
    }
}
