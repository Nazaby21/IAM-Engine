package io.sala.krob_krong.classroom.controller;

import io.sala.krob_krong.classroom.dto.request.CreateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.request.UpdateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.response.ClassroomResponseDto;
import io.sala.krob_krong.classroom.service.ClassroomService;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schools")
public class ClassroomController {

    private final ClassroomService classroomService;

    @PostMapping("/{branchId}/classrooms")
    public ResponseEntity<ClassroomResponseDto> createClassroom(
            @Valid @RequestBody CreateClassroomRequestDto requestDto,
            @PathVariable UUID branchId){
        ClassroomResponseDto responseDto = classroomService.createClassroom(branchId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);

    }

    @PatchMapping("/{branchId}/classrooms/{classroomId}")
    public ResponseEntity<ClassroomResponseDto> updateClassroomById(
            @PathVariable UUID branchId,
            @PathVariable UUID classroomId,
            @Valid @RequestBody UpdateClassroomRequestDto requestDto){
        ClassroomResponseDto responseDto = classroomService.updateClassroomById(branchId,classroomId,requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/classrooms/{classroomId}")
    public ResponseEntity<String> deleteClassroomById(@PathVariable  UUID classroomId){
        classroomService.deleteClassroomById(classroomId);
        return ResponseEntity.ok("Classroom deleted successfully");
    }

    @GetMapping("/classrooms/{classroomId}")
    public ResponseEntity<ClassroomResponseDto> getClassroomById(@PathVariable UUID classroomId){
        ClassroomResponseDto responseDto = classroomService.getClassroomById(classroomId);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/classrooms/{branchId}/branch")
    public ResponseEntity<List<ClassroomResponseDto>> getAllClassroomByBranchId(@PathVariable UUID branchId){
        List<ClassroomResponseDto> responseDto = classroomService.getAllClassroomByBranchId(branchId);
        return ResponseEntity.ok(responseDto);
    }

}
