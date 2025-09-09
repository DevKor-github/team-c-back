package devkor.com.teamcback.domain.vote.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import static devkor.com.teamcback.domain.place.entity.QPlace.place;
import static devkor.com.teamcback.domain.vote.entity.QVoteOption.voteOption;
import static devkor.com.teamcback.domain.vote.entity.QVoteRecord.voteRecord;
import static devkor.com.teamcback.domain.vote.entity.QVote.vote;

import devkor.com.teamcback.domain.place.entity.Place;
import devkor.com.teamcback.domain.place.entity.PlaceType;
import devkor.com.teamcback.domain.vote.dto.response.GetVoteOptionRes;
import devkor.com.teamcback.domain.vote.dto.response.QGetVoteOptionRes;
import devkor.com.teamcback.domain.vote.entity.Vote;
import devkor.com.teamcback.domain.vote.entity.VoteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomVoteRepositoryImpl implements CustomVoteRepository{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<GetVoteOptionRes> getVoteOptionsByPlaceByVoteTopicIdAndPlaceId(Long voteTopicId, Long placeId) {
        return jpaQueryFactory
                .select(
                        new QGetVoteOptionRes(
                                voteOption.id,
                                voteOption.optionText,
                                voteRecord.id.count().intValue()
                        )
                )
                .from(vote)
                .join(voteOption)
                .on(vote.voteTopicId.eq(voteOption.voteTopicId))
                .leftJoin(voteRecord)
                .on(voteRecord.voteId.eq(vote.id)
                        .and(voteRecord.voteOptionId.eq(voteOption.id)))
                .where(
                        vote.voteTopicId.eq(voteTopicId)
                                .and(vote.placeId.eq(placeId))
                )
                .groupBy(voteOption.id, voteOption.optionText)
                .orderBy(
                        voteOption.id.asc()
                )
                .fetch();
    }

    @Override
    public List<Vote> getVoteByStatusWithPage(VoteStatus status, Long lastVoteId, int size) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(gtVoteCursor(lastVoteId));

        if (status != null) {
            builder.and(vote.status.eq(status));
        }

        return jpaQueryFactory
                .selectFrom(vote)
                .where(
                        gtVoteCursor(lastVoteId).and(builder)
                )
                .orderBy(
                        vote.id.asc()
                )
                .limit(size)
                .fetch();
    }

    @Override
    public List<GetVoteOptionRes> getVoteOptionByVoteId(Long voteId) {
        return jpaQueryFactory
                .select(
                        new QGetVoteOptionRes(
                                voteOption.id,
                                voteOption.optionText,
                                voteRecord.id.count().intValue()
                        )
                )
                .from(vote)
                .join(voteOption)
                .on(vote.voteTopicId.eq(voteOption.voteTopicId))
                .leftJoin(voteRecord)
                .on(voteRecord.voteId.eq(vote.id)
                        .and(voteRecord.voteOptionId.eq(voteOption.id)))
                .where(
                        vote.id.eq(voteId)
                )
                .groupBy(voteOption.id, voteOption.optionText)
                .orderBy(
                        voteOption.id.asc()
                )
                .fetch();
    }

    private BooleanExpression gtVoteCursor(Long lastVoteId) {
        if (lastVoteId== null) return null;
        return vote.id.gt(lastVoteId);
    }
}
