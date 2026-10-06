package io.sala.krob_krong.school.service;

import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;
import io.sala.krob_krong.school.dto.*;
import java.util.*;

public interface SchoolService {
    SchoolResponse register(SchoolProfileRequest request);

    SchoolResponse get(UUID schoolId);

    PageResponse<SchoolSummaryResponse> mine(PageRequest page);

    SchoolResponse update(UUID schoolId, SchoolProfileRequest request);

    OnboardingResponse onboarding(UUID schoolId);

    SchoolResponse submit(UUID schoolId);

    PageResponse<SchoolResponse> directory(String status, PageRequest page);

    List<SchoolReviewResponse> reviews(UUID schoolId);

    SchoolResponse review(UUID schoolId, ReviewDecisionRequest request);

    SchoolResponse suspend(UUID schoolId, String reason);

    SchoolResponse reinstate(UUID schoolId);
}
