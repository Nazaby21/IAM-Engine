package io.sala.krob_krong.attendance.controller;

import io.sala.krob_krong.attendance.dto.request.AttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.request.CreateAttendanceRequestDto;
import io.sala.krob_krong.attendance.dto.response.AttendanceResponseDto;
import io.sala.krob_krong.attendance.dto.response.CreateAttendanceResponseDto;
import io.sala.krob_krong.attendance.service.AttendanceService;
import io.sala.krob_krong.common.response.ApiResponse;
import io.sala.krob_krong.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AttendanceController {
    private final AttendanceService attendanceService;

    @PostMapping("/attendance")
    public ApiResponse<CreateAttendanceResponseDto> attendanceRecords(
            @Valid @RequestBody CreateAttendanceRequestDto request) {
        return ApiResponse.create(attendanceService.recordAttendance(request));
    }

    @PutMapping("/attendance/update")
    public ApiResponse<AttendanceResponseDto> updateAttendanceRecords(
            @Valid @RequestBody AttendanceRequestDto request) {
        return ApiResponse.success(attendanceService.updateAttendance(request));
    }

    @GetMapping("/attendance/student/{student_id}")
    public ApiResponse<List<AttendanceResponseDto>> getAttendanceRecords(@PathVariable("student_id") UUID studentId) {
        return ApiResponse.success(attendanceService.getAttendance(studentId));
    }

    @GetMapping("/attendance")
    public ApiResponse<PageResponse<AttendanceResponseDto>> getAllAttendanceRecords(
            @RequestParam(required = false) UUID search,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AttendanceResponseDto> responseDtoPage = attendanceService.searchAttendance(search, pageable);

        PageResponse<AttendanceResponseDto> pageResponse = PageResponse.from(responseDtoPage);
        return ApiResponse.paginated(pageResponse);
    }
}
