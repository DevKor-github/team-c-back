package devkor.com.teamcback.domain.review.repository;

import devkor.com.teamcback.domain.common.entity.File;

import java.util.List;

public interface CustomFileRepository {
    List<File> getReviewFilesByPlaceWithPage(Long placeId, Long lastFileId, int size);
}
