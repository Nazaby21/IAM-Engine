package io.sala.krob_krong.classroom.service;

import io.sala.krob_krong.classroom.dto.request.CreateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.request.UpdateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.response.ClassroomResponseDto;

import java.util.List;
import java.util.UUID;

public interface ClassroomService {

    ClassroomResponseDto createClassroom(UUID branchId, CreateClassroomRequestDto requestDto);
    List<ClassroomResponseDto> getAllClassroomByBranchId(UUID branchId);
    ClassroomResponseDto getClassroomById(UUID classroomId);
    ClassroomResponseDto updateClassroomById(UUID branchId, UUID classroomId, UpdateClassroomRequestDto requestDto);
    void deleteClassroomById(UUID classroomId);
}
