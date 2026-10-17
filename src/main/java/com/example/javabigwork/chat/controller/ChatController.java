package com.example.javabigwork.chat.controller;

import com.example.javabigwork.chat.dto.ChatRequest;
import com.example.javabigwork.chat.dto.ChatResponse;
import com.example.javabigwork.chat.dto.ConversationView;
import com.example.javabigwork.chat.entity.Message;
import com.example.javabigwork.chat.service.ChatService;
import com.example.javabigwork.chat.service.ConversationService;
import com.example.javabigwork.agent.dto.CoachFeedback.TargetWord;
import com.example.javabigwork.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final ChatService chatService;
    private final ConversationService conversationService;

    public ChatController(ChatService chatService, ConversationService conversationService) {
        this.chatService = chatService;
        this.conversationService = conversationService;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(chatService.chat(request));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationView>> conversations() {
        return ApiResponse.ok(conversationService.list());
    }

    @GetMapping("/chat/practice-context")
    public ApiResponse<List<TargetWord>> practiceContext() {
        return ApiResponse.ok(chatService.practiceContext());
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<Message>> messages(@PathVariable Long id) {
        return ApiResponse.ok(chatService.messages(id));
    }
}
