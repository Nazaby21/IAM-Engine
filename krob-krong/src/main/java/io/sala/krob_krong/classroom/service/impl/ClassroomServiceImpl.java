package io.sala.krob_krong.classroom.service.impl;

import io.sala.krob_krong.branch.entity.BranchEntity;
import io.sala.krob_krong.branch.repository.BranchRepository;
import io.sala.krob_krong.classroom.constants.ClassType;
import io.sala.krob_krong.classroom.dto.request.CreateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.request.UpdateClassroomRequestDto;
import io.sala.krob_krong.classroom.dto.response.ClassroomResponseDto;
import io.sala.krob_krong.classroom.entity.ClassroomEntity;
import io.sala.krob_krong.classroom.mapper.ClassroomMapper;
import io.sala.krob_krong.classroom.repository.ClassroomRepository;
import io.sala.krob_krong.classroom.service.ClassroomService;
import io.sala.krob_krong.common.exceptions.BusinessException;
import io.sala.krob_krong.iam.error.IAMErrorCode;
import io.sala.krob_krong.school.entity.SchoolEntity;
import io.sala.krob_krong.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    private final ClassroomMapper classroomMapper;
    private final ClassroomRepository classroomRepository;
    private final BranchRepository branchRepository;
    private final SchoolRepository schoolRepository;

    @Override
    public ClassroomResponseDto createClassroom(UUID branchId, CreateClassroomRequestDto requestDto) {

        validateFloor(requestDto.getClassType(), requestDto.getFloor());

        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.BRANCH_NOT_FOUND));

        SchoolEntity school = schoolRepository.findById(branch.getSchoolId())
                .orElseThrow(() -> new BusinessException(IAMErrorCode.SCHOOL_NOT_FOUND));

        ClassroomEntity classroom = classroomMapper.toEntity(requestDto);
        classroom.setBranchId(branch.getId());
        classroom.setSchoolId(branch.getSchoolId());
        classroom.setCreatedBy(currentUserId());
        ClassroomEntity saved = classroomRepository.save(classroom);
        return classroomMapper.toResponse(saved,school.getDisplayName());
    }

    @Override
    public List<ClassroomResponseDto> getAllClassroomByBranchId(UUID branchId) {
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.BRANCH_NOT_FOUND));
        SchoolEntity school = schoolRepository.findById(branch.getSchoolId())
                .orElseThrow(() -> new BusinessException(IAMErrorCode.SCHOOL_NOT_FOUND));
        return classroomRepository.findAllByBranchIdOrderByIdAsc(branchId)
                .stream()
                .map(classroom -> classroomMapper.toResponse(classroom,school.getDisplayName()))
                .toList();
    }

    @Override
    public ClassroomResponseDto getClassroomById(UUID classroomId) {

        ClassroomEntity classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.CLASSROOM_NOT_FOUND));
        SchoolEntity school = schoolRepository.findById(classroom.getSchoolId())
                .orElseThrow(() -> new BusinessException(IAMErrorCode.SCHOOL_NOT_FOUND));

        return classroomMapper.toResponse(classroom,school.getDisplayName());

    }

    @Override
    @Transactional
    public ClassroomResponseDto updateClassroomById(UUID branchId, UUID classroomId,
                                                    UpdateClassroomRequestDto requestDto) {
        validateFloor(requestDto.getClassType(), requestDto.getFloor());
        if(!branchRepository.existsById(branchId)){
            throw new BusinessException(IAMErrorCode.BRANCH_NOT_FOUND);
        }
        ClassroomEntity classroom = classroomRepository.findByIdAndBranchId(classroomId, branchId)
                .orElseThrow(() -> new BusinessException(IAMErrorCode.CLASSROOM_NOT_FOUND));
        SchoolEntity school = schoolRepository.findById(classroom.getSchoolId())
                .orElseThrow(() -> new BusinessException(IAMErrorCode.SCHOOL_NOT_FOUND));
        classroomMapper.updateEntity(requestDto, classroom);
        classroom.setCreatedBy(currentUserId());

        ClassroomEntity saved = classroomRepository.save(classroom);
        return classroomMapper.toResponse(saved, school.getDisplayName());
    }

    @Override
    @Transactional
    public void deleteClassroomById(UUID classroomId) {
        if(!classroomRepository.existsById(classroomId)){
            throw new BusinessException(IAMErrorCode.CLASSROOM_NOT_FOUND);
        }
        classroomRepository.deleteById(classroomId);
    }

    private UUID currentUserId() {
        return UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"); // seed user temp
    }
    private void validateFloor(ClassType classType, String floor){
        if(classType == ClassType.OFFLINE
        && !StringUtils.hasText(floor)){
            throw new BusinessException(IAMErrorCode.REQUIRED_FLOOR);
        }
        if (classType == ClassType.ONLINE
                && StringUtils.hasText(floor)) {
            throw new BusinessException(IAMErrorCode.NOT_REQUIRED_FLOOR);
        }
    }
}
