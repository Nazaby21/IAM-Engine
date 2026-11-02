package io.sala.krob_krong.classroom.controller;

import io.sala.krob_krong.classroom.dto.request.CreateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.request.UpdateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.response.ClassroomResponseDto;
import io.sala.krob_krong.classroom.service.ClassroomService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schools")
public class ClassroomController {

    private final ClassroomService classroomService;

    @PostMapping("/{branch_id}/classrooms")
    public ResponseEntity<ClassroomResponseDto> createClassroom(
            @Valid @RequestBody CreateClassroomRequestDto requestDto, @PathVariable("branch_id") UUID branchId) {
        ClassroomResponseDto responseDto = classroomService.createClassroom(branchId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PatchMapping("/{branch_id}/classrooms/{classroom_id}")
    public ResponseEntity<ClassroomResponseDto> updateClassroomById(
            @PathVariable("branch_id") UUID branchId,
            @PathVariable("classroom_id") UUID classroomId,
            @Valid @RequestBody UpdateClassroomRequestDto requestDto) {
        ClassroomResponseDto responseDto = classroomService.updateClassroomById(branchId, classroomId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/classrooms/{classroom_id}")
    public ResponseEntity<String> deleteClassroomById(@PathVariable("classroom_id") UUID classroomId) {
        classroomService.deleteClassroomById(classroomId);
        return ResponseEntity.ok("Classroom deleted successfully");
    }

    @GetMapping("/classrooms/{classroom_id}")
    public ResponseEntity<ClassroomResponseDto> getClassroomById(@PathVariable("classroom_id") UUID classroomId) {
        ClassroomResponseDto responseDto = classroomService.getClassroomById(classroomId);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/classrooms/branch/{branch_id}")
    public ResponseEntity<List<ClassroomResponseDto>> getAllClassroomByBranchId(
            @PathVariable("branch_id") UUID branchId) {
        List<ClassroomResponseDto> responseDto = classroomService.getAllClassroomByBranchId(branchId);
        return ResponseEntity.ok(responseDto);
    }
}
