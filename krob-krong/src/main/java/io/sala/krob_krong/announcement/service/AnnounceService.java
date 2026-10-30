package io.sala.krob_krong.announcement.service;

import io.sala.krob_krong.announcement.constant.AnnounceStatus;
import io.sala.krob_krong.announcement.dto.request.AnnounceMakeRequest;
import io.sala.krob_krong.announcement.dto.response.AnnounceResponse;
import io.sala.krob_krong.common.request.PageRequest;
import io.sala.krob_krong.common.response.PageResponse;

public interface AnnounceService {
    AnnounceResponse make(AnnounceMakeRequest request);

    AnnounceResponse viewDetail(String id);

    PageResponse<AnnounceResponse> view(PageRequest page, AnnounceStatus status);
}