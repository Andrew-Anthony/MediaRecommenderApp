package ro.anthony.mediarecommenderapp.dto.chat;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatRequest {
    private List<ChatMessage> messages;
}
