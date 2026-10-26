package io.sala.krob_krong.announcement.controller;

import io.sala.krob_krong.announcement.dto.request.AnnounceMakeRequest;
import io.sala.krob_krong.announcement.dto.response.AnnounceResponse;
import io.sala.krob_krong.announcement.service.AnnounceService;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/announces")
public class AnnounceController {
    private final AnnounceService announceService;

    @PostMapping
    public ApiResponse<AnnounceResponse> makeAnnouncement(@Valid @RequestBody AnnounceMakeRequest request) {
        AnnounceResponse response = announceService.make(request);
        return ApiResponse.create(response);
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<AnnounceResponse> viewDetail(@PathVariable String id){
        AnnounceResponse response = announceService.viewDetail(id);
        return ApiResponse.success(response);
    }

    @GetMapping
    public  ApiResponse<PageResponse<AnnounceResponse>> viewAll(PageRequest page){
        page.setSize(10);
        PageResponse<AnnounceResponse> response = announceService.view(page);
        return ApiResponse.success(response);
    }
}
