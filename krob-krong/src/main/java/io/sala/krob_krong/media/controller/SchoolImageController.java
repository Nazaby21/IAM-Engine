package io.sala.krob_krong.media.controller;

import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.iam.security.RequirePermission;
import io.sala.krob_krong.media.dto.SchoolImageResponse;
import io.sala.krob_krong.media.service.SchoolImageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schools/{school_id}")
public class SchoolImageController {
    private final SchoolImageService images;

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(value = "school.profile:edit", school = "#schoolId")
    public ApiResponse<SchoolImageResponse> logo(
            @PathVariable("school_id") UUID schoolId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.create(images.upload(schoolId, "school_logo", file));
    }

    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(value = "school.profile:edit", school = "#schoolId")
    public ApiResponse<SchoolImageResponse> cover(
            @PathVariable("school_id") UUID schoolId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.create(images.upload(schoolId, "school_cover", file));
    }

    @GetMapping("/media/{asset_id}/content")
    @RequirePermission(value = "media.public:view", school = "#schoolId")
    public ResponseEntity<byte[]> content(
            @PathVariable("school_id") UUID schoolId, @PathVariable("asset_id") UUID assetId) {
        var image = images.download(schoolId, assetId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(image.mimeType()))
                .body(image.bytes());
    }
}
