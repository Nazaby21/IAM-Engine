package io.sala.krob_krong.event.controller;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.event.dto.request.EventCreateRequestDto;
import io.sala.krob_krong.event.dto.response.CountdownResponse;
import io.sala.krob_krong.event.dto.response.EventResponseDto;
import io.sala.krob_krong.event.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    @GetMapping
    public ApiResponse<PageResponse<EventResponseDto>> listAll(PageRequest page){
        page.setSize(10);
        return ApiResponse.success(eventService.listAll(page));
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<EventResponseDto> listById(@PathVariable UUID id){
        return ApiResponse.success(eventService.listById(id));
    }

    @PostMapping("/{school_id}")
    public ApiResponse <EventResponseDto> create(@PathVariable("school_id") UUID schoolId, @Valid @RequestBody EventCreateRequestDto request){
        return ApiResponse.create(eventService.create(schoolId, request));
    }

    @GetMapping("/{id}/countdown")
    public ApiResponse<CountdownResponse> countDown(@PathVariable UUID id){
        return ApiResponse.success(eventService.getCountdown(id));
    }

}
