package devkor.com.teamcback.domain.review.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import devkor.com.teamcback.domain.common.entity.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static devkor.com.teamcback.domain.common.entity.QFile.file;
import static devkor.com.teamcback.domain.review.entity.QReview.review;

@Repository
@RequiredArgsConstructor
public class CustomFileRepositoryImpl implements CustomFileRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<File> getReviewFilesByPlaceWithPage(Long placeId, Long lastFileId, int size) {
        return jpaQueryFactory
                .select(file)
                .from(file)
                .join(review).on(file.fileUuid.eq(review.fileUuid))
                .where(
                        review.place.id.eq(placeId),
                        file.id.gt(lastFileId)
                )
                .orderBy(
                        review.createdAt.desc(),
                        file.sortNum.asc()
                )
                .limit(size)
                .fetch();
    }
}
