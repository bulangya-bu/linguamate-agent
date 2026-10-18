package com.example.javabigwork.mission.controller;

import com.example.javabigwork.common.ApiResponse;
import com.example.javabigwork.mission.dto.MissionView;
import com.example.javabigwork.mission.service.MissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/missions")
public class MissionController {
    private final MissionService missions;
    public MissionController(MissionService missions) {this.missions=missions;}
    public record Version(@Min(0) int version) {}
    public record WordAnswer(@Min(0) int version,@Min(0) @Max(2) int index,@NotBlank @Size(max=120) String answer) {}
    public record Step(@Min(0) int version,@Min(0) @Max(2) int step) {}
    public record Expression(@Min(0) int version,@Min(0) @Max(2) int step,@NotBlank @Size(max=2000) String answer) {}
    public record Reflection(@Min(0) int version,@NotBlank @Size(max=1000) String reflection) {}
    @GetMapping("/today") public ApiResponse<List<MissionView>> today() {return ApiResponse.ok(missions.today());}
    @GetMapping("/history") public ApiResponse<List<MissionView>> history() {return ApiResponse.ok(missions.history());}
    @PostMapping("/{key}/start") public ApiResponse<MissionView> start(@PathVariable String key) {return ApiResponse.ok(missions.start(key));}
    @GetMapping("/sessions/{id}") public ApiResponse<MissionView> get(@PathVariable long id) {return ApiResponse.ok(missions.get(id));}
    @PostMapping("/sessions/{id}/read") public ApiResponse<MissionView> read(@PathVariable long id,@Valid @RequestBody Version body) {return ApiResponse.ok(missions.read(id,body.version()));}
    @PostMapping("/sessions/{id}/words") public ApiResponse<MissionView> word(@PathVariable long id,@Valid @RequestBody WordAnswer body) {return ApiResponse.ok(missions.answerWord(id,body.version(),body.index(),body.answer()));}
    @PostMapping("/sessions/{id}/hint") public ApiResponse<MissionView> hint(@PathVariable long id,@Valid @RequestBody Step body) {return ApiResponse.ok(missions.hint(id,body.version(),body.step()));}
    @PostMapping("/sessions/{id}/turns") public ApiResponse<MissionView> turn(@PathVariable long id,@Valid @RequestBody Expression body) {return ApiResponse.ok(missions.answerTurn(id,body.version(),body.step(),body.answer()));}
    @PostMapping("/sessions/{id}/finish") public ApiResponse<MissionView> finish(@PathVariable long id,@Valid @RequestBody Reflection body) {return ApiResponse.ok(missions.finish(id,body.version(),body.reflection()));}
}
